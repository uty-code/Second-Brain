package com.aimsgraph.domain.wiki;

import com.aimsgraph.domain.wiki.mapper.WikiPageMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zettelkasten Wiki 서비스. 물리 파일 시스템(SST)과 MSSQL(캐시 저장소), Neo4j(지식 그래프) 간의 하이브리드 캐싱 및 양방향 동기화를 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiService {

  private final WikiPageMapper wikiPageMapper;
  private final RedissonClient redissonClient;
  private final Neo4jClient neo4jClient;

  @Value("${wiki.base.dir:workspaces}")
  private String wikiBaseDir;

  private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9-_]+$");

  /** Cache-first로 위키 페이지 본문을 가져오고, DB 캐시 미스 시 디스크에서 Lazy Loading 합니다. */
  @Transactional
  public String getWikiContent(String workspaceId, String conceptName) throws IOException {
    // 1. DB 캐시에서 Title로 1차 조회 (레이턴시 최적화)
    WikiPage cachedPage = wikiPageMapper.findByWorkspaceIdAndTitle(workspaceId, conceptName);
    if (cachedPage != null
        && cachedPage.getContent() != null
        && !cachedPage.getContent().isEmpty()) {
      log.info("Cache Hit (DB) for concept: {}", conceptName);
      return cachedPage.getContent();
    }

    // 2. Slug 확인 및 2차 로컬 파일 탐색 준비
    String slug = null;
    if (conceptName != null && NAME_PATTERN.matcher(conceptName).matches()) {
      slug = conceptName;
    }

    Path wsPath = Paths.get(wikiBaseDir, workspaceId, "wiki").toAbsolutePath().normalize();
    Path rootWikiPath = Paths.get("wiki").toAbsolutePath().normalize();

    Path targetFile = null;
    if (slug != null) {
      targetFile = findFile(wsPath, slug);
      if (targetFile == null) {
        targetFile = findFile(rootWikiPath, slug);
      }
    }

    // 3. 한글 매핑 등을 위한 Neo4j Slug 변환 후 3차 탐색
    if (targetFile == null) {
      slug = resolveSlug(workspaceId, conceptName);
      if (slug != null && NAME_PATTERN.matcher(slug).matches()) {
        targetFile = findFile(wsPath, slug);
        if (targetFile == null) {
          targetFile = findFile(rootWikiPath, slug);
        }
      }
    }

    if (targetFile == null) {
      log.warn("Wiki file not found for: {} in workspace: {}", conceptName, workspaceId);
      throw new java.io.FileNotFoundException("The requested wiki page could not be found.");
    }

    // 4. Cache Miss - 로컬 파일에서 읽기
    log.info("Cache Miss. Loading from physical file: {}", targetFile);
    String fileContent = Files.readString(targetFile);

    // 5. DB 캐시 데이터 생성/갱신 (Lazy Loading)
    String relativePath = wsPath.relativize(targetFile).toString().replace("\\", "/");
    String pageType =
        relativePath.startsWith("concepts/")
            ? "CONCEPT"
            : relativePath.startsWith("entities/") ? "ENTITY" : "INSIGHT";

    WikiPage wikiPage = new WikiPage();
    wikiPage.setWorkspaceId(workspaceId);
    wikiPage.setPagePath(relativePath);
    wikiPage.setTitle(conceptName);
    wikiPage.setPageType(pageType);
    wikiPage.setContent(fileContent);
    wikiPage.setContentHash(calculateHash(fileContent));

    wikiPageMapper.mergeWikiPage(wikiPage);

    return fileContent;
  }

  /**
   * 웹 UI에서 저장/수정 시 호출: DB 캐시 갱신 + 물리 파일 덮어쓰기 + Neo4j 관계 갱신 (양방향 동기화)
   *
   * <p><b>[아키텍처적 Trade-off 설계 주석]</b><br>
   * 프록시 기반 {@code @Transactional}로 인해 메서드 진입 시점에 트랜잭션이 시작됩니다. {@code Files.writeString} 이후 DB 처리 중
   * 장애가 나면 DB 트랜잭션은 롤백되나 물리 파일 변경은 복구되지 않는 일시적인 불일치가 일어날 수 있습니다.<br>
   * 이 시스템은 파일 시스템이 <b>SST(Single Source of Truth)</b>이며, 백그라운드의 <b>Self-Healing Daemon(자가 치유 지식
   * 린터)</b>이 주기적으로 파일과 RDBMS 간의 해시 델타를 검출해 DB 캐시를 보정(Heal)하므로 최종적 일관성(Eventual Consistency) 모델 하에 이
   * 불일치 위험을 수용합니다.
   */
  @Transactional
  public void saveOrUpdateWiki(
      String workspaceId, String conceptName, String pageType, String content) throws IOException {
    String lockKey = "lock:wiki:" + workspaceId;
    RLock lock = redissonClient.getLock(lockKey);

    try {
      // 가상 스레드 캐리어 스레드 피닝(Pinning) 방지를 위한 Redisson RLock 사용
      boolean isLocked = lock.tryLock(10, 30, TimeUnit.SECONDS);
      if (!isLocked) {
        throw new RuntimeException("Could not acquire lock for wiki file update");
      }

      String slug = conceptName.trim().toLowerCase().replaceAll("[^a-z0-9\\-]", "-");
      String relativePath = pageType.toLowerCase() + "s/" + slug + ".md";
      Path targetFile =
          Paths.get(wikiBaseDir, workspaceId, "wiki", relativePath).toAbsolutePath().normalize();

      // 1. 물리 파일 덮어쓰기
      Files.createDirectories(targetFile.getParent());
      Files.writeString(
          targetFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

      // 2. MSSQL 캐시 업데이트
      WikiPage wikiPage = new WikiPage();
      wikiPage.setWorkspaceId(workspaceId);
      wikiPage.setPagePath(relativePath);
      wikiPage.setTitle(conceptName);
      wikiPage.setPageType(pageType);
      wikiPage.setContent(content);
      wikiPage.setContentHash(calculateHash(content));
      wikiPageMapper.mergeWikiPage(wikiPage);

      // 3. Neo4j 관계 및 노드 정보 동적 업데이트
      neo4jClient
          .query(
              "MERGE (c:Concept {name: $slug, workspaceId: $workspaceId}) "
                  + "SET c.title = $title, c.updatedAt = datetime()")
          .bind(slug)
          .to("slug")
          .bind(workspaceId)
          .to("workspaceId")
          .bind(conceptName)
          .to("title")
          .run();

      log.info("Successfully updated wiki: {} (DB and File System)", conceptName);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Failed to acquire lock due to interruption", e);
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  /** 외부 편집기(Obsidian 등)로 인해 로컬 파일 변경 시 DB 캐시 반영 */
  @Transactional
  public void syncFileToDb(String workspaceId, String pagePath, String fileContent) {
    String contentHash = calculateHash(fileContent);

    // 기존 DB 정보를 조회해본 뒤, 변경이 있을 때만 갱신
    WikiPage existing = wikiPageMapper.findByWorkspaceIdAndPagePath(workspaceId, pagePath);
    if (existing == null || !contentHash.equals(existing.getContentHash())) {
      log.info("Syncing changed file to DB: {} in workspace: {}", pagePath, workspaceId);

      String title = pagePath.substring(pagePath.lastIndexOf("/") + 1).replace(".md", "");
      String pageType =
          pagePath.startsWith("concepts/")
              ? "CONCEPT"
              : pagePath.startsWith("entities/") ? "ENTITY" : "INSIGHT";

      WikiPage wikiPage = new WikiPage();
      wikiPage.setWorkspaceId(workspaceId);
      wikiPage.setPagePath(pagePath);
      wikiPage.setTitle(title);
      wikiPage.setPageType(pageType);
      wikiPage.setContent(fileContent);
      wikiPage.setContentHash(contentHash);

      wikiPageMapper.mergeWikiPage(wikiPage);
    }
  }

  private String resolveSlug(String workspaceId, String conceptName) {
    try {
      return neo4jClient
          .query(
              "MATCH (c:Concept {workspaceId: $workspaceId}) "
                  + "WHERE c.name = $conceptName OR c.title = $conceptName "
                  + "RETURN c.name AS name LIMIT 1")
          .bind(workspaceId)
          .to("workspaceId")
          .bind(conceptName)
          .to("conceptName")
          .fetchAs(String.class)
          .first()
          .orElse(null);
    } catch (Exception e) {
      log.error("Failed to resolve slug from Neo4j", e);
      return null;
    }
  }

  private Path findFile(Path basePath, String conceptName) {
    if (!Files.exists(basePath)) {
      return null;
    }
    Path conceptPath =
        basePath.resolve("concepts/" + conceptName + ".md").toAbsolutePath().normalize();
    Path entityPath =
        basePath.resolve("entities/" + conceptName + ".md").toAbsolutePath().normalize();
    Path insightPath =
        basePath.resolve("insights/" + conceptName + ".md").toAbsolutePath().normalize();

    if (Files.exists(conceptPath) && conceptPath.startsWith(basePath)) return conceptPath;
    if (Files.exists(entityPath) && entityPath.startsWith(basePath)) return entityPath;
    if (Files.exists(insightPath) && insightPath.startsWith(basePath)) return insightPath;
    return null;
  }

  /**
   * SHA-256 기반 본문 해시 산출
   *
   * <p><b>[가상 스레드 CPU-bound 점유 분석 주석]</b><br>
   * 본 연산은 암호화 해싱으로 스레드를 블로킹하지 않고 연산을 수행하므로 가상 스레드 환경에서 캐리어 스레드를 점유합니다. 그러나 Zettelkasten 마크다운 텍스트
   * 크기가 평균 수 킬로바이트 수준으로 극히 작아, 점유 속도가 나노초 단위에 준하여 가상 스레드 스케줄링 오버헤드를 유발하지 않고 안전하게 동작함을 인지하고 설계되었습니다.
   */
  private String calculateHash(String content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }
}
