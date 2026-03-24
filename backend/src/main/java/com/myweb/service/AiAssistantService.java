package com.myweb.service;

import com.myweb.dto.SearchItemDTO;
import com.myweb.entity.ChatMessage;
import com.myweb.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AiAssistantService {
    private final SearchService searchService;
    private final ChatMessageRepository chatMessageRepository;

    public AiAssistantService(SearchService searchService, ChatMessageRepository chatMessageRepository) {
        this.searchService = searchService;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public AiStreamResult buildAnswer(String sessionId, String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        String sid = (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId.trim();
        List<SearchItemDTO> refs = searchService.search(question, null, 0, 3).getItems();
        StringBuilder builder = new StringBuilder();
        builder.append("基于站内内容，我的回答如下：");
        if (refs.isEmpty()) {
            builder.append("当前没有检索到直接匹配内容，请尝试更具体的关键词。");
        } else {
            for (int i = 0; i < refs.size(); i++) {
                SearchItemDTO ref = refs.get(i);
                builder.append("\n").append(i + 1).append(". ").append(ref.getTitle());
                builder.append("（").append(ref.getUrl()).append("）");
            }
            builder.append("\n建议先查看以上来源，再继续追问具体实现细节。");
        }
        String answer = builder.toString();
        saveMessage(sid, "user", question);
        saveMessage(sid, "assistant", answer);
        return new AiStreamResult(sid, answer, refs);
    }

    private void saveMessage(String sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        chatMessageRepository.save(message);
    }

    public record AiStreamResult(String sessionId, String answer, List<SearchItemDTO> sources) {
        public List<String> chunkedAnswer() {
            String[] words = answer.split("(?<=\\G.{18})");
            List<String> chunks = new ArrayList<>();
            for (String word : words) {
                if (!word.isBlank()) chunks.add(word);
            }
            return chunks;
        }
    }
}
