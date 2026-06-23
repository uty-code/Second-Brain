package com.aimsgraph.ingest;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.aimsgraph.domain.workspace.WorkspaceService;
import com.aimsgraph.domain.workspace.WorkspaceCredentialsService;

public class LlmExtractionQualityTest {

    @Test
    public void testExtractionQuality() throws Exception {
        // Mock dependencies
        WorkspaceService workspaceService = Mockito.mock(WorkspaceService.class);
        WorkspaceCredentialsService credentialsService = Mockito.mock(WorkspaceCredentialsService.class);
        Neo4jClient neo4jClient = Mockito.mock(Neo4jClient.class, Mockito.RETURNS_DEEP_STUBS);
        NotionIngestService notionIngestService = Mockito.mock(NotionIngestService.class);

        // Mock Neo4jClient behavior using deep stubs
        Mockito.when(neo4jClient.query(Mockito.anyString()).bind(Mockito.any()).to(Mockito.anyString()).fetch().all())
               .thenReturn(Collections.emptyList());

        // Create LlmService instance manually
        LlmService llmService = new LlmService(workspaceService, credentialsService, neo4jClient, notionIngestService);
        ReflectionTestUtils.setField(llmService, "defaultApiKey", System.getenv("OPENAI_API_KEY") != null ? System.getenv("OPENAI_API_KEY") : "demo");
        ReflectionTestUtils.setField(llmService, "wikiBaseDir", "workspaces");

        // Sample Long Markdown Text about RAG
        String sampleText = """
        # Retrieval-Augmented Generation (RAG) 개념과 장점

        ## 1. RAG의 정의
        RAG(Retrieval-Augmented Generation)는 대규모 언어 모델(LLM)의 생성 능력을 외부 지식 베이스 검색과 결합한 기술입니다.
        모델이 학습한 파라미터 내의 정보에만 의존하지 않고, 질문을 받을 때마다 관련 문서나 데이터를 실시간으로 검색(Retrieval)하여
        이를 바탕으로 답변을 생성(Generation)합니다.

        ## 2. RAG의 핵심 구성 요소
        - **문서 청킹(Chunking) 및 임베딩(Embedding)**: 방대한 문서를 의미 단위로 쪼개고, 이를 벡터 공간에 매핑합니다.
        - **벡터 데이터베이스(Vector DB)**: 임베딩된 벡터 데이터를 저장하고, 사용자 질의와 가장 유사한 청크를 빠르게 검색합니다. 대표적으로 Pinecone, Milvus 등이 있습니다.
        - **생성 모델(Generator)**: 검색된 컨텍스트를 프롬프트에 주입하여, 최종적으로 사용자가 이해하기 쉬운 자연어 형태의 답변을 생성합니다.

        ## 3. RAG의 장점
        - **환각(Hallucination) 감소**: 모델이 모르는 정보를 지어내는 현상을 크게 줄일 수 있습니다. 근거 기반의 답변이 가능합니다.
        - **최신 정보 유지**: LLM을 새로 재학습(Fine-tuning)하지 않아도, 외부 데이터베이스만 업데이트하면 항상 최신 지식을 활용할 수 있습니다.
        - **출처 추적 가능**: 답변 생성에 사용된 문서의 출처를 명확히 제시할 수 있어 신뢰성이 높습니다.
        """;

        // Access private method via reflection
        Method method = LlmService.class.getDeclaredMethod("callResponsesAPI", List.class, String.class, String.class, String.class);
        method.setAccessible(true);

        System.out.println("==========================================");
        System.out.println("Starting LLM Extraction...");
        System.out.println("==========================================");
        
        long startTime = System.currentTimeMillis();
        String result = (String) method.invoke(llmService, null, sampleText, "test-workspace", "gpt-4o-mini");
        long endTime = System.currentTimeMillis();

        System.out.println("==========================================");
        System.out.println("Extraction Complete in " + (endTime - startTime) + "ms");
        System.out.println("Result:");
        System.out.println(result);
        System.out.println("==========================================");
    }
}
