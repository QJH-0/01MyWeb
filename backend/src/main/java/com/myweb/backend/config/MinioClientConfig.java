package com.myweb.backend.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 非 test Profile 才创建真实 MinIO 客户端，避免 CI/H2 测试去连本地 9000。
 */
@Configuration
@Profile("!test")
public class MinioClientConfig {

    @Bean
    MinioClient minioClient(FileStorageProperties properties) {
        FileStorageProperties.MinioProperties m = properties.minio();
        MinioClient.Builder b = MinioClient.builder().endpoint(m.endpoint())
                .credentials(m.accessKey(), m.secretKey());
        if (m.region() != null && !m.region().isBlank()) {
            b.region(m.region());
        }
        return b.build();
    }
}
