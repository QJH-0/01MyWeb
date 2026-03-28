package com.myweb.backend.storage;

import com.myweb.backend.common.ApiException;
import com.myweb.backend.common.ErrorCodes;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试专用内存存储：不依赖 MinIO，支撑文件接口的集成测试。
 */
@Component
@Profile("test")
public class InMemoryFileStorage implements FileStoragePort {
    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public void putObject(String key, InputStream stream, long sizeBytes, String contentType) throws IOException {
        byte[] data = readAll(stream);
        objects.put(key, data);
    }

    @Override
    public InputStream getObject(String key) {
        byte[] data = objects.get(key);
        if (data == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "object not found");
        }
        return new ByteArrayInputStream(data);
    }

    @Override
    public void removeObject(String key) {
        objects.remove(key);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        in.transferTo(buf);
        return buf.toByteArray();
    }
}
