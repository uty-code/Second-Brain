package com.aimsgraph.domain.source;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimsgraph.outbox.OutboxEventMapper;
import com.aimsgraph.testcontainers.AbstractContainerBaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RawSourceServiceTest extends AbstractContainerBaseTest {

  @Autowired private RawSourceService rawSourceService;

  @Autowired private RawSourceMapper rawSourceMapper;

  @Autowired private OutboxEventMapper outboxEventMapper;

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
    RawSource savedSource =
        rawSourceMapper.findByWorkspaceIdAndUri("test-workspace", "s3://test/doc.md");
    assertThat(savedSource).isNotNull();

    var events = outboxEventMapper.findByWorkspaceId("test-workspace");
    assertThat(events).hasSize(1);
    assertThat(events.get(0).getAggregateType()).isEqualTo("DOCUMENT");
    assertThat(events.get(0).getEventType()).isEqualTo("CREATED");
    assertThat(events.get(0).getStatus()).isEqualTo("PENDING");
  }
}
