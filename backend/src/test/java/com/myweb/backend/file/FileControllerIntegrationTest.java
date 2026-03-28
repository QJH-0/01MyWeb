package com.myweb.backend.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 文件上传/列表/软删/匿名下载：存储层在 test Profile 使用内存实现。 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadListDownloadDeleteFlow() throws Exception {
        String token = loginAsAdmin();
        byte[] png = new byte[]{ (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };

        MvcResult uploaded = mockMvc.perform(multipart("/api/admin/files/upload")
                        .file(new MockMultipartFile("file", "hi.png", "image/png", png))
                        .param("folder", "docs")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("hi.png"))
                .andExpect(jsonPath("$.data.fileType").value("image/png"))
                .andExpect(jsonPath("$.data.accessUrl").exists())
                .andReturn();

        long id = objectMapper.readTree(uploaded.getResponse().getContentAsString()).at("/data/id").asLong();

        mockMvc.perform(get("/api/admin/files")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(id));

        mockMvc.perform(get("/api/files/" + id + "/download"))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsByteArray()).isEqualTo(png));

        mockMvc.perform(delete("/api/admin/files/" + id)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/files/" + id + "/download"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        mockMvc.perform(get("/api/admin/files")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void rejectsUnsupportedContentType() throws Exception {
        String token = loginAsAdmin();
        mockMvc.perform(multipart("/api/admin/files/upload")
                        .file(new MockMultipartFile("file", "x.bin", "application/octet-stream", new byte[]{ 1, 2, 3 }))
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsInvalidListFileType() throws Exception {
        String token = loginAsAdmin();
        mockMvc.perform(get("/api/admin/files")
                        .param("fileType", "not-a-mime")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsBadFolder() throws Exception {
        String token = loginAsAdmin();
        mockMvc.perform(multipart("/api/admin/files/upload")
                        .file(new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes()))
                        .param("folder", "../etc")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    private String loginAsAdmin() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"admin",
                                  "password":"Admin12345"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return body.at("/data/accessToken").asText();
    }
}
