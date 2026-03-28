package com.myweb.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对象存储与上传约束：密钥来自环境变量；本地默认对接 compose 中的 MinIO。
 */
@ConfigurationProperties(prefix = "app.storage")
public record FileStorageProperties(
        String publicApiBaseUrl,
        long maxFileSizeBytes,
        boolean physicalDeleteAfterSoftDelete,
        MinioProperties minio
) {
    public record MinioProperties(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket,
            String region
    ) {
    }
}
