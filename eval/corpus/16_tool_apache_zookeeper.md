# Apache ZooKeeper의 분산 코디네이션 서비스

Apache ZooKeeper는 대규모 분산 시스템에서 설정 관리, 리더 선출(Leader Election), 동기화 락(Distributed Lock), 네임스페이스 관리를 중앙에서 일관되게 조정하기 위해 개발된 고가용성 코디네이터입니다.
데이터를 표준 파일 시스템과 유사한 계층형 트리 구조(Znode)로 메모리에 저장합니다.
클라이언트는 특정 노드에 와처(Watcher)를 등록하여 노드의 생성, 수정, 삭제 이벤트를 실시간으로 통보받을 수 있습니다.
ZAB(ZooKeeper Atomic Broadcast) 합의 프로토콜을 기반으로 쿼럼(Quorum, 과반수 노드) 합의를 통해 강력한 데이터 일관성을 유지합니다.
카프카(과거 버전), 하둡, HBase 등 수많은 빅데이터 분산 클러스터의 두뇌 역할을 해왔습니다.
