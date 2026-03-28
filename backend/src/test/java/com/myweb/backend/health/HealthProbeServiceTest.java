package com.myweb.backend.health;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 探针单元测试：不可达端点映射为 false 与 UNAVAILABLE，不抛到调用方。 */
class HealthProbeServiceTest {

    @Test
    void shouldReturnUnavailableWhenAllEndpointsUnreachable() {
        HealthProbeProperties.Endpoint endpoint = new HealthProbeProperties.Endpoint("localhost", 1);
        HealthProbeProperties properties = new HealthProbeProperties(
                endpoint,
                endpoint,
                endpoint,
                endpoint,
                100,
                100
        );

        HealthProbeService service = new HealthProbeService(properties);
        HealthStatus status = service.probeAll();

        assertThat(status.mysql()).isFalse();
        assertThat(status.redis()).isFalse();
        assertThat(status.elasticsearch()).isFalse();
        assertThat(status.minio()).isFalse();
        assertThat(status.elasticsearchStatus()).isEqualTo("UNAVAILABLE");
    }
}
