package com.aimsgraph.ingest.eval;

public enum KnowledgeProvenance {
  /** 원문에서 명백하게 직접 뒷받침되는 사실 */
  SOURCE_SUPPORTED,

  /** 금지된 프로젝트 고유어, 사칭 패키지, 날조된 벤치마크 등 명백한 허위 사실 */
  UNSUPPORTED_OR_FABRICATED,

  /** 원문에는 없으나 널리 알려진 표준 아키텍처/패턴 설명 */
  GENERAL_KNOWLEDGE_CANDIDATE,

  /** 자동 판정이 모호하여 사람의 맥락 검토가 필요한 항목 */
  MANUAL_REVIEW_REQUIRED
}
