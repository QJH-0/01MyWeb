package com.myweb.backend.storage;

import com.myweb.backend.common.ApiException;
import com.myweb.backend.common.ErrorCodes;
import com.myweb.backend.config.FileStorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * MinIO 适配：启动时确保桶存在；读写异常映射为业务可见的错误码，避免把 SDK 细节返回给客户端。
 */
@Component
@Profile("!test")
public class MinioFileStorage implements FileStoragePort {
    private static final Logger log = LoggerFactory.getLogger(MinioFileStorage.class);
    private static final long DEFAULT_PART = 10 * 1024 * 1024;

    private final MinioClient minioClient;
    private final FileStorageProperties properties;

    public MinioFileStorage(MinioClient minioClient, FileStorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @PostConstruct
    void ensureBucket() throws Exception {
        String bucket = properties.minio().bucket();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("MinIO bucket created: {}", bucket);
        }
    }

    @Override
    public void putObject(String key, InputStream stream, long sizeBytes, String contentType) throws IOException {
        String bucket = properties.minio().bucket();
        long size = sizeBytes;
        long partSize = DEFAULT_PART;
        if (size < 0) {
            size = -1;
        }
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .stream(stream, size, partSize)
                            .contentType(contentType)
                            .build()
            );
        } catch (ErrorResponseException e) {
            throw storageWriteFailure(e);
        } catch (Exception e) {
            throw new IOException("MinIO put failed", e);
        }
    }

    @Override
    public InputStream getObject(String key) throws IOException {
        String bucket = properties.minio().bucket();
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "object not found");
            }
            log.warn("MinIO get failed, key={}", key, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "storage read failed");
        } catch (Exception e) {
            throw new IOException("MinIO get failed", e);
        }
    }

    @Override
    public void removeObject(String key) {
        String bucket = properties.minio().bucket();
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            log.warn("MinIO remove failed, key={}", key, e);
        }
    }

    private static IOException storageWriteFailure(ErrorResponseException e) {
        return new IOException("MinIO put rejected: " + e.errorResponse().code(), e);
    }
}
