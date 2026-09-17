package com.aimsgraph.testcontainers;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractContainerBaseTest {

  protected static final MSSQLServerContainer<?> mssql;
  protected static final Neo4jContainer<?> neo4j;
  protected static final GenericContainer<?> redis;
  protected static final KafkaContainer kafka;

  static {
    mssql =
        new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    neo4j = new Neo4jContainer<>("neo4j:5-community").withAdminPassword("password");

    redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    // 컨테이너 구동
    mssql.start();
    neo4j.start();
    redis.start();
    kafka.start();
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    // MSSQL
    registry.add("spring.datasource.url", mssql::getJdbcUrl);
    registry.add("spring.datasource.username", mssql::getUsername);
    registry.add("spring.datasource.password", mssql::getPassword);
    registry.add("spring.sql.init.mode", () -> "always");

    // Neo4j
    registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
    registry.add("spring.neo4j.authentication.username", () -> "neo4j");
    registry.add("spring.neo4j.authentication.password", () -> "password");

    // Redis
    registry.add("spring.redis.host", redis::getHost);
    registry.add("spring.redis.port", () -> redis.getMappedPort(6379));

    // Kafka
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
  }
}
