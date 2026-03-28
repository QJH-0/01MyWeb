package com.myweb.backend.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** ES 客户端与 SearchIndexOperations 实现切换（启用 ES / 显式关闭）。 */
@Configuration
public class ElasticsearchClientConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
    RestClient restClient(SearchProperties props) {
        var es = props.getElasticsearch();
        return RestClient.builder(new HttpHost(es.getHost(), es.getPort(), "http")).build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
    ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    @Bean
    @ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
    ElasticsearchClient elasticsearchClient(ElasticsearchTransport elasticsearchTransport) {
        return new ElasticsearchClient(elasticsearchTransport);
    }

    @Bean
    @ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
    SearchIndexOperations elasticsearchSearchIndexService(ElasticsearchClient elasticsearchClient, SearchProperties props) {
        return new ElasticsearchSearchIndexService(elasticsearchClient, props);
    }

    @Bean
    @ConditionalOnProperty(name = "app.search.elasticsearch.enabled", havingValue = "false")
    SearchIndexOperations disabledSearchIndexService() {
        return new DisabledSearchIndexService();
    }
}
