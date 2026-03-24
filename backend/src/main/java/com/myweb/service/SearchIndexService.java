package com.myweb.service;

import com.myweb.entity.Blog;
import com.myweb.entity.BlogStatus;
import com.myweb.entity.Project;
import com.myweb.entity.SearchDocument;
import com.myweb.entity.SearchOutboxEvent;
import com.myweb.repository.BlogRepository;
import com.myweb.repository.ProjectRepository;
import com.myweb.repository.SearchDocumentRepository;
import com.myweb.repository.SearchOutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SearchIndexService {
    private static final Logger log = LoggerFactory.getLogger(SearchIndexService.class);
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_FAILED = "FAILED";
    private static final int RETRY_LIMIT = 5;

    private final SearchOutboxEventRepository outboxRepository;
    private final SearchDocumentRepository searchDocumentRepository;
    private final BlogRepository blogRepository;
    private final ProjectRepository projectRepository;

    public SearchIndexService(
        SearchOutboxEventRepository outboxRepository,
        SearchDocumentRepository searchDocumentRepository,
        BlogRepository blogRepository,
        ProjectRepository projectRepository
    ) {
        this.outboxRepository = outboxRepository;
        this.searchDocumentRepository = searchDocumentRepository;
        this.blogRepository = blogRepository;
        this.projectRepository = projectRepository;
    }

    @Scheduled(fixedDelayString = "${app.search.consume-delay-ms:2000}")
    @Transactional
    public void consumePendingEvents() {
        processPendingEvents();
    }

    @Transactional
    public int processPendingEvents() {
        List<SearchOutboxEvent> events = outboxRepository.findTop100ByStatusOrderByIdAsc(STATUS_PENDING);
        for (SearchOutboxEvent event : events) {
            try {
                applyEvent(event);
                event.setStatus(STATUS_DONE);
            } catch (Exception ex) {
                int nextRetry = event.getRetryCount() + 1;
                event.setRetryCount(nextRetry);
                event.setStatus(nextRetry >= RETRY_LIMIT ? STATUS_FAILED : STATUS_PENDING);
                log.warn("search_outbox_consume_failed, eventId={}, retryCount={}, reason={}",
                    event.getId(), nextRetry, ex.getMessage());
            }
        }
        outboxRepository.saveAll(events);
        return events.size();
    }

    @Transactional
    public void rebuildAll() {
        searchDocumentRepository.deleteAllInBatch();
        blogRepository.findAll().forEach(this::upsertBlogDocument);
        projectRepository.findAll().forEach(this::upsertProjectDocument);
    }

    private void applyEvent(SearchOutboxEvent event) {
        String aggregateType = event.getAggregateType();
        Long aggregateId = event.getAggregateId();
        if ("BLOG".equals(aggregateType)) {
            if (event.getEventType().name().equals("DELETE")) {
                searchDocumentRepository.deleteById(docId("blog", aggregateId));
            } else {
                Optional<Blog> blogOpt = blogRepository.findById(aggregateId);
                blogOpt.ifPresent(this::upsertBlogDocument);
            }
            return;
        }
        if ("PROJECT".equals(aggregateType)) {
            if (event.getEventType().name().equals("DELETE")) {
                searchDocumentRepository.deleteById(docId("project", aggregateId));
            } else {
                Optional<Project> projectOpt = projectRepository.findById(aggregateId);
                projectOpt.ifPresent(this::upsertProjectDocument);
            }
        }
    }

    private void upsertBlogDocument(Blog blog) {
        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            searchDocumentRepository.deleteById(docId("blog", blog.getId()));
            return;
        }
        SearchDocument doc = new SearchDocument();
        doc.setId(docId("blog", blog.getId()));
        doc.setSourceType("blog");
        doc.setSourceId(blog.getId());
        doc.setTitle(blog.getTitle());
        doc.setSummary(blog.getSummary());
        doc.setContent(blog.getTitle() + " " + blog.getSummary() + " " + blog.getContent());
        doc.setUrl("/blog/" + blog.getId());
        searchDocumentRepository.save(doc);
    }

    private void upsertProjectDocument(Project project) {
        if (!project.isVisible()) {
            searchDocumentRepository.deleteById(docId("project", project.getId()));
            return;
        }
        SearchDocument doc = new SearchDocument();
        doc.setId(docId("project", project.getId()));
        doc.setSourceType("project");
        doc.setSourceId(project.getId());
        doc.setTitle(project.getTitle());
        doc.setSummary(project.getSummary());
        doc.setContent(project.getTitle() + " " + project.getSummary() + " " + String.join(" ", project.getTags()));
        doc.setUrl("/projects");
        searchDocumentRepository.save(doc);
    }

    private String docId(String sourceType, Long sourceId) {
        return sourceType + ":" + sourceId;
    }
}
