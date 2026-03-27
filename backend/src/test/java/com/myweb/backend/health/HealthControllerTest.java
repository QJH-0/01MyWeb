package com.myweb.backend.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
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
