# 인프라 환경 셋업 가이드 (SETUP.md)

이 프로젝트는 Docker Compose를 기반으로 로컬 인프라를 한 번에 띄웁니다.

## 1. 요구 사항
- Docker & Docker Compose
- Java 21 (JDK)
- Gradle 8.x

## 2. Docker Compose 서비스 정의
`AIMS-Graph-Backend/docker-compose.yml`에 아래 서비스들을 정의합니다.

| 서비스 | 이미지 | 포트 | 볼륨 |
|---|---|---|---|
| `mssql` | `mcr.microsoft.com/mssql/server:2022-latest` | `1433:1433` | `mssql-data:/var/opt/mssql` |
| `neo4j` | `neo4j:5-community` | `7474:7474`, `7687:7687` | `neo4j-data:/data` |
| `zookeeper` | `confluentinc/cp-zookeeper:7.6.0` | `2181:2181` | - |
| `kafka` | `confluentinc/cp-kafka:7.6.0` | `9092:9092` | - |
| `redis` | `redis:7-alpine` | `6300:6379` | - |

## 3. 필수 환경 변수 (`.env`)
개발 환경에서 애플리케이션을 구동하기 위한 필수 환경 변수 목록입니다.
```env
# MSSQL
SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=AIMSGraph;encrypt=true;trustServerCertificate=true;
SPRING_DATASOURCE_USERNAME=sa
SPRING_DATASOURCE_PASSWORD=YourStrong!Passw0rd

# Neo4j
SPRING_NEO4J_URI=bolt://localhost:7687
SPRING_NEO4J_AUTHENTICATION_USERNAME=neo4j
SPRING_NEO4J_AUTHENTICATION_PASSWORD=password

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6300
```

### 3.2 LLM API 키
```env
# OpenAI API Key (시스템 환경 변수로 주입 가능)
OPENAI_API_KEY=sk-your-api-key-here
```

## 4. 실행 방법
```bash
# 1. 인프라 구동
docker-compose up -d

# 2. Kafka 토픽 초기 생성 (최초 1회)
docker exec -it kafka kafka-topics --create --topic aims.outbox.events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# 3. 백엔드 애플리케이션 빌드 및 실행
./gradlew bootRun
```

## 5. Spring Profile 분리
| Profile | 용도 | 활성화 방법 |
|---|---|---|
| `local` | 로컬 개발 (Docker Compose 인프라) | `SPRING_PROFILES_ACTIVE=local` |
| `test` | 테스트 (Testcontainers 사용) | 자동 적용 |
| `prod` | 운영 배포 | `SPRING_PROFILES_ACTIVE=prod` |
