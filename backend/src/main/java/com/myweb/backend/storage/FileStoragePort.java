package com.myweb.backend.storage;

import java.io.IOException;
import java.io.InputStream;

/** 对象存储抽象：便于测试用内存实现替换 MinIO，不在业务层依赖 SDK 类型。 */
public interface FileStoragePort {

    void putObject(String key, InputStream stream, long sizeBytes, String contentType) throws IOException;

    /**
     * @return 调用方必须在读完或异常后关闭流。
     */
    InputStream getObject(String key) throws IOException;

    void removeObject(String key);
}
