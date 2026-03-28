package com.myweb.backend.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.myweb.backend.common.ApiException;
import com.myweb.backend.common.ErrorCodes;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.dto.SearchItemDTO;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Elasticsearch 8.x 实现：索引 myweb_content、multi_match 检索与 highlight。 */
public class ElasticsearchSearchIndexService implements SearchIndexOperations {

    private static final String MAPPING_JSON = """
            {
              "mappings": {
                "properties": {
                  "source_type": { "type": "keyword" },
                  "source_id": { "type": "long" },
                  "title": { "type": "text" },
                  "title_keyword": { "type": "keyword" },
                  "summary": { "type": "text" },
                  "content": { "type": "text" },
                  "category": { "type": "keyword" },
                  "tags": { "type": "keyword" },
                  "url": { "type": "keyword" },
                  "published_at": { "type": "date" },
                  "created_at": { "type": "date" },
                  "updated_at": { "type": "date" }
                }
              }
            }
            """;

    private final ElasticsearchClient client;
    private final String indexName;

    public ElasticsearchSearchIndexService(ElasticsearchClient client, SearchProperties props) {
        this.client = client;
        this.indexName = props.getIndexName();
    }

    static String documentId(String sourceType, long sourceId) {
        return sourceType + "_" + sourceId;
    }

    @Override
    public void ensureIndex() {
        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                return;
            }
            client.indices().create(c -> c.index(indexName).withJson(new StringReader(MAPPING_JSON)));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "Failed to ensure search index");
        }
    }

    @Override
    public void deleteIndex() {
        try {
            if (client.indices().exists(e -> e.index(indexName)).value()) {
                client.indices().delete(d -> d.index(indexName));
            }
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "Failed to delete search index");
        }
    }

    @Override
    public void deleteDocument(String sourceType, long sourceId) {
        try {
            String id = documentId(sourceType, sourceId);
            if (client.exists(e -> e.index(indexName).id(id)).value()) {
                client.delete(d -> d.index(indexName).id(id).refresh(Refresh.True));
            }
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "Failed to delete search document");
        }
    }

    @Override
    public void indexBlogDocument(
            long id,
            String title,
            String summary,
            String content,
            String category,
            List<String> tags,
            String url,
            Instant publishedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("source_type", "blog");
        doc.put("source_id", id);
        doc.put("title", title);
        doc.put("title_keyword", title);
        doc.put("summary", summary);
        doc.put("content", content == null ? "" : content);
        doc.put("category", category == null ? "" : category);
        doc.put("tags", tags == null ? List.of() : tags);
        doc.put("url", url);
        doc.put("published_at", publishedAt == null ? null : publishedAt.toString());
        doc.put("created_at", createdAt == null ? null : createdAt.toString());
        doc.put("updated_at", updatedAt == null ? null : updatedAt.toString());
        indexDocument(doc, documentId("blog", id));
    }

    @Override
    public void indexProjectDocument(
            long id,
            String title,
            String summary,
            String description,
            String category,
            List<String> tags,
            String url,
            Instant createdAt,
            Instant updatedAt
    ) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("source_type", "project");
        doc.put("source_id", id);
        doc.put("title", title);
        doc.put("title_keyword", title);
        doc.put("summary", summary);
        doc.put("content", description == null ? "" : description);
        doc.put("category", category == null ? "" : category);
        doc.put("tags", tags == null ? List.of() : tags);
        doc.put("url", url);
        doc.put("published_at", null);
        doc.put("created_at", createdAt == null ? null : createdAt.toString());
        doc.put("updated_at", updatedAt == null ? null : updatedAt.toString());
        indexDocument(doc, documentId("project", id));
    }

    private void indexDocument(Map<String, Object> doc, String id) {
        try {
            ensureIndex();
            client.index(i -> i.index(indexName).id(id).document(doc).refresh(Refresh.True));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "Failed to index document");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public PagedResult<SearchItemDTO> search(String query, String typeFilter, int page, int limit) {
        try {
            ensureIndex();
            int from = page * limit;
            SearchResponse<Map> response = client.search(s -> s
                            .index(indexName)
                            .from(from)
                            .size(limit)
                            .query(q -> q.bool(b -> {
                                b.must(m -> m.multiMatch(mm -> mm
                                        .query(query)
                                        .fields("title^2", "summary", "content")
                                        .type(TextQueryType.BestFields)));
                                if (typeFilter != null && !typeFilter.isBlank()) {
                                    b.filter(f -> f.term(t -> t.field("source_type").value(typeFilter)));
                                }
                                return b;
                            }))
                            .highlight(h -> h
                                    .preTags("<mark>")
                                    .postTags("</mark>")
                                    .fields("title", hf -> hf.numberOfFragments(2).fragmentSize(160))
                                    .fields("summary", hf -> hf.numberOfFragments(2).fragmentSize(160))
                                    .fields("content", hf -> hf.numberOfFragments(1).fragmentSize(200))
                            ),
                    Map.class
            );
            TotalHits totalHits = response.hits().total();
            long total = totalHits == null ? 0L : totalHits.value();
            List<SearchItemDTO> items = response.hits().hits().stream()
                    .map(this::mapHit)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return new PagedResult<>(items, total, page, limit);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "Search failed");
        }
    }

    private SearchItemDTO mapHit(Hit<Map> hit) {
        Map<String, Object> src = hit.source();
        if (src == null) {
            return null;
        }
        Object st = src.get("source_type");
        Object sid = src.get("source_id");
        if (st == null || sid == null) {
            return null;
        }
        String sourceType = st.toString();
        long sourceId = ((Number) sid).longValue();
        String title = stringVal(src.get("title"));
        String url = stringVal(src.get("url"));
        String summary = stringVal(src.get("summary"));
        List<String> highlights = new ArrayList<>();
        Map<String, List<String>> hl = hit.highlight();
        if (hl != null) {
            for (List<String> frags : hl.values()) {
                if (frags != null) {
                    highlights.addAll(frags);
                }
            }
        }
        return new SearchItemDTO(sourceType, sourceId, title, url, summary, highlights.isEmpty() ? List.of() : highlights);
    }

    private static String stringVal(Object o) {
        return o == null ? "" : o.toString();
    }
}
