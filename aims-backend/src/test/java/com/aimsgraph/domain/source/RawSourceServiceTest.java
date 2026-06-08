package com.aimsgraph.domain.source;

import com.aimsgraph.outbox.OutboxEventMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RawSourceServiceTest {

    @Container
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
            .acceptLicense();

    @DynamicPropertySource
    static void mssqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mssql::getJdbcUrl);
        registry.add("spring.datasource.username", mssql::getUsername);
        registry.add("spring.datasource.password", mssql::getPassword);
        registry.add("spring.sql.init.mode", () -> "always");
    }

    @Autowired
    private RawSourceService rawSourceService;

    @Autowired
    private RawSourceMapper rawSourceMapper;

    @Autowired
    private OutboxEventMapper outboxEventMapper;

    @Test
    void shouldSaveRawSourceAndOutboxEventInSameTransaction() {
        // given
        RawSource source = new RawSource();
        source.setWorkspaceId("test-workspace");
        source.setSourceUri("s3://test/doc.md");
        source.setTitle("Test Doc");
        source.setContentHash("hash123");
        source.setSourceType("MARKDOWN");
        source.setStatus("RECEIVED");

        // when
        rawSourceService.ingestSource(source);

        // then
        RawSource savedSource = rawSourceMapper.findByWorkspaceIdAndUri("test-workspace", "s3://test/doc.md");
        assertThat(savedSource).isNotNull();

        var events = outboxEventMapper.findByWorkspaceId("test-workspace");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAggregateType()).isEqualTo("DOCUMENT");
        assertThat(events.get(0).getEventType()).isEqualTo("CREATED");
        assertThat(events.get(0).getStatus()).isEqualTo("PENDING");
    }
}
