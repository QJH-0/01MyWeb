package com.myweb.backend.health;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 依赖就绪探针：TCP 可达性 + ES HTTP 摘要解析；失败不抛异常到控制器层，以 false/UNAVAILABLE 呈现。
 */
@Service
public class HealthProbeService {

    private static final String ES_CLUSTER_HEALTH_PATH = "/_cluster/health";
    private static final Pattern ES_STATUS_PATTERN = Pattern.compile("\"status\"\\s*:\\s*\"(green|yellow|red)\"");

    private final HealthProbeProperties properties;
    private final HttpClient httpClient;

    /**
     * 构造健康探针服务。
     *
     * @param properties 健康探针配置属性
     */
    public HealthProbeService(HealthProbeProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                .build();
    }

    /**
     * 探测所有依赖服务状态。
     * 包括 MySQL、Redis、Elasticsearch 和 MinIO。
     *
     * @return 健康状态对象
     */
    public HealthStatus probeAll() {
        boolean mysql = isTcpReachable(properties.mysql());
        boolean redis = isTcpReachable(properties.redis());
        boolean elasticsearch = isTcpReachable(properties.elasticsearch());
        boolean minio = isTcpReachable(properties.minio());

        String elasticsearchStatus = elasticsearch ? fetchElasticsearchStatus() : "UNAVAILABLE";
        return new HealthStatus(mysql, redis, elasticsearch, minio, elasticsearchStatus);
    }

    /**
     * 检查 TCP 端口是否可达。
     *
     * @param endpoint 端点配置（包含主机名和端口）
     * @return true 如果连接成功，false 如果连接失败
     */
    private boolean isTcpReachable(HealthProbeProperties.Endpoint endpoint) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.host(), endpoint.port()), properties.connectTimeoutMs());
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * 获取 Elasticsearch 集群健康状态。
     * 通过 HTTP 请求获取 ES 集群状态。
     *
     * @return 集群状态字符串（green/yellow/red）或 "UNAVAILABLE"
     */
    private String fetchElasticsearchStatus() {
        URI uri = URI.create("http://" + properties.elasticsearch().host() + ":" + properties.elasticsearch().port() + ES_CLUSTER_HEALTH_PATH);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(properties.readTimeoutMs()))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "UNAVAILABLE";
            }
            Matcher matcher = ES_STATUS_PATTERN.matcher(response.body());
            return matcher.find() ? matcher.group(1) : "UNAVAILABLE";
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "UNAVAILABLE";
        } catch (IOException ex) {
            return "UNAVAILABLE";
        }
    }
}
