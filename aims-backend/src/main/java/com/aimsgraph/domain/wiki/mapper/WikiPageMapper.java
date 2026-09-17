package com.aimsgraph.domain.wiki.mapper;

import com.aimsgraph.domain.wiki.WikiPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WikiPageMapper {

  /** 워크스페이스 ID와 타이틀로 위키 페이지 캐시 조회 */
  WikiPage findByWorkspaceIdAndTitle(
      @Param("workspaceId") String workspaceId, @Param("title") String title);

  /** 워크스페이스 ID와 페이지 경로(파일명 포함)로 위키 페이지 캐시 조회 */
  WikiPage findByWorkspaceIdAndPagePath(
      @Param("workspaceId") String workspaceId, @Param("pagePath") String pagePath);

  /** 신규 위키 페이지 생성 */
  void insertWikiPage(WikiPage wikiPage);

  /** 기존 위키 페이지 정보 업데이트 (캐시 본문 포함) */
  void updateWikiPage(WikiPage wikiPage);

  /** 위키 페이지 Upsert (있으면 UPDATE, 없으면 INSERT) */
  void mergeWikiPage(WikiPage wikiPage);
}
