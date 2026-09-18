# Neo4j 그래프 데이터베이스와 Cypher 쿼리 언어

Neo4j는 데이터를 노드(Node), 관계(Relationship), 속성(Property)으로 표현하는 세계에서 가장 널리 쓰이는 네이티브 그래프 데이터베이스(LPG, Labeled Property Graph)입니다.
관계형 DB가 복잡한 다대다 관계를 표현할 때 조인 테이블(Join Table)로 인해 성능이 기하급수적으로 저하되는 것과 달리, Neo4j는 인덱스 없는 인접성(Index-free Adjacency)을 통해 노드 간의 포인터를 직접 추적하므로 수백만 건의 관계를 상수 시간(O(1))에 순회(Traversal)할 수 있습니다.
SQL과 유사한 선언형 패턴 매칭 언어인 Cypher를 사용하여 `(u:User)-[:FOLLOWS]->(f:User)`와 같이 직관적으로 지식 그래프를 질의할 수 있습니다.
소셜 네트워크, 추천 시스템, 지식 그래프(Knowledge Graph), 사기 탐지(Fraud Detection)에 최적화되어 있습니다.
