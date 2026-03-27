package com.myweb.backend.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.probes.mysql.host=localhost",
        "app.probes.mysql.port=1",
        "app.probes.redis.host=localhost",
        "app.probes.redis.port=1",
        "app.probes.elasticsearch.host=localhost",
        "app.probes.elasticsearch.port=1",
        "app.probes.minio.host=localhost",
        "app.probes.minio.port=1",
        "app.probes.connect-timeout-ms=100",
        "app.probes.read-timeout-ms=100"
})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnApiResponseWithTraceId() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.elasticsearchStatus").value("UNAVAILABLE"));
    }

    @Test
    void shouldEchoIncomingTraceId() throws Exception {
        mockMvc.perform(get("/api/health").header("X-Trace-Id", "trace-test-001"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "trace-test-001"))
                .andExpect(jsonPath("$.traceId").value("trace-test-001"));
    }
}
