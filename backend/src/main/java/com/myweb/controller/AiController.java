package com.myweb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myweb.dto.SearchItemDTO;
import com.myweb.service.AiAssistantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiAssistantService aiAssistantService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AiController(AiAssistantService aiAssistantService, ObjectMapper objectMapper) {
        this.aiAssistantService = aiAssistantService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/chat/stream")
    public SseEmitter stream(
        @RequestParam("q") String question,
        @RequestParam(name = "sessionId", required = false) String sessionId
    ) {
        SseEmitter emitter = new SseEmitter(30_000L);
        AiAssistantService.AiStreamResult result = aiAssistantService.buildAnswer(sessionId, question);
        List<String> chunks = result.chunkedAnswer();
        executor.execute(() -> {
            try {
                for (String chunk : chunks) {
                    emitter.send(SseEmitter.event().name("delta").data(chunk));
                    Thread.sleep(80L);
                }
                emitter.send(SseEmitter.event().name("done").data(toJson(Map.of(
                    "sessionId", result.sessionId(),
                    "sources", result.sources().stream().map(this::toSource).toList()
                ))));
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(ex.getMessage()));
                } catch (Exception ignore) {
                    // ignore secondary send errors
                }
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    private Map<String, Object> toSource(SearchItemDTO item) {
        return Map.of(
            "sourceType", item.getSourceType(),
            "sourceId", item.getSourceId(),
            "title", item.getTitle(),
            "url", item.getUrl()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
