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

@Service
public class ContentQueryService {
    private final ContentPageRepository contentPageRepository;
    private final ObjectMapper objectMapper;

    public ContentQueryService(ContentPageRepository contentPageRepository, ObjectMapper objectMapper) {
        this.contentPageRepository = contentPageRepository;
        this.objectMapper = objectMapper;
    }

    public ContentPageDTO getPageByKey(String pageKey) {
        ContentPageEntity page = contentPageRepository.findByPageKey(pageKey)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Content page not found"));

        JsonNode sections = parseSections(page.getSectionsJson());
        return new ContentPageDTO(page.getTitle(), page.getSummary(), sections, page.getUpdatedAt());
    }

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

