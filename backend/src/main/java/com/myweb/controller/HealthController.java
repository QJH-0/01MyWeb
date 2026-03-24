package com.myweb.controller;

import com.myweb.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.slf4j.MDC;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final RestTemplate restTemplate;

    private final String elasticsearchBaseUrl;
    private final String minioBaseUrl;

    public HealthController(
        DataSource dataSource,
        RedisConnectionFactory redisConnectionFactory,
        RestTemplate restTemplate,
        @Value("${app.elasticsearch.baseUrl:http://localhost:9200}") String elasticsearchBaseUrl,
        @Value("${app.minio.baseUrl:http://localhost:9000}") String minioBaseUrl
    ) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
        this.restTemplate = restTemplate;
        this.elasticsearchBaseUrl = elasticsearchBaseUrl;
        this.minioBaseUrl = minioBaseUrl;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthVO>> health(HttpServletRequest request) {
        String traceId = request.getHeader(com.myweb.common.TraceIdFilter.TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get("traceId");
        }

        HealthVO vo = new HealthVO();
        vo.setMysql(checkMysql());
        vo.setRedis(checkRedis());

        ElasticsearchCheck es = checkElasticsearch();
        vo.setElasticsearch(es.ok());
        vo.setElasticsearchStatus(es.status());

        vo.setMinio(checkMinio());

        boolean allOk = vo.isMysql() && vo.isRedis() && vo.isElasticsearch() && vo.isMinio();
        if (allOk) {
            return ResponseEntity.ok(ApiResponse.ok(vo, traceId));
        }
        return ResponseEntity.ok(ApiResponse.fail("服务未完全就绪（开发模式允许最终一致）", vo, traceId));
    }

    private boolean checkMysql() {
        try {
            dataSource.getConnection().close();
            return true;
        } catch (DataAccessException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRedis() {
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            String pong = conn.ping();
            return pong != null && !"".equals(pong);
        } catch (Exception e) {
            return false;
        }
    }

    private ElasticsearchCheck checkElasticsearch() {
        try {
            String url = elasticsearchBaseUrl + "/_cluster/health?local=true";
            // 只做可达性/返回结构校验，具体字段在后续模块精化
            restTemplate.getForEntity(url, String.class);
            return ElasticsearchCheck.ok("reachable");
        } catch (RestClientException e) {
            return ElasticsearchCheck.fail(e.getMessage());
        } catch (Exception e) {
            return ElasticsearchCheck.fail(e.getMessage());
        }
    }

    private boolean checkMinio() {
        try {
            String url = minioBaseUrl + "/minio/health/live";
            restTemplate.getForEntity(url, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private record ElasticsearchCheck(boolean ok, String status) {
        static ElasticsearchCheck ok(String status) {
            return new ElasticsearchCheck(true, status);
        }

        static ElasticsearchCheck fail(String status) {
            return new ElasticsearchCheck(false, status);
        }
    }
}

