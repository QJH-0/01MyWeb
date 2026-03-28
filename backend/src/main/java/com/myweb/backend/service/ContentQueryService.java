package com.myweb.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.backend.common.ApiException;
import com.myweb.backend.common.ErrorCodes;
import com.myweb.backend.dto.ContentPageDTO;
import com.myweb.backend.entity.ContentPageEntity;
import com.myweb.backend.repository.ContentPageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 内容页查询：`sections` 以 JSON 存库，对外解析为 {@link com.fasterxml.jackson.databind.JsonNode} 数组。
 */
@Service
public class ContentQueryService {
    private final ContentPageRepository contentPageRepository;
    private final ObjectMapper objectMapper;

    public ContentQueryService(ContentPageRepository contentPageRepository, ObjectMapper objectMapper) {
        this.contentPageRepository = contentPageRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据页面 key 查询内容页。
     *
     * @param pageKey 页面唯一标识（如 "home", "about", "experience"）
     * @return 内容页 DTO
     * @throws ApiException 如果页面不存在
     */
    public ContentPageDTO getPageByKey(String pageKey) {
        ContentPageEntity page = contentPageRepository.findByPageKey(pageKey)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Content page not found"));

        JsonNode sections = parseSections(page.getSectionsJson());
        return new ContentPageDTO(page.getTitle(), page.getSummary(), sections, page.getUpdatedAt());
    }

    /**
     * 解析 JSON 格式的 sections 字符串为 JsonNode。
     * 如果 sectionsJson 为空或无效，返回空数组节点。
     *
     * @param sectionsJson JSON 字符串
     * @return 解析后的 JsonNode（数组类型）
     * @throws ApiException 如果 JSON 解析失败
     */
    private JsonNode parseSections(String sectionsJson) {
        if (sectionsJson == null || sectionsJson.isBlank()) {
            return objectMapper.createArrayNode();
        }
        try {
            JsonNode node = objectMapper.readTree(sectionsJson);
            return node == null ? objectMapper.createArrayNode() : node;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "Invalid content page sections");
        }
    }
}
