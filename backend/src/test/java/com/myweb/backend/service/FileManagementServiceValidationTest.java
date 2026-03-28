package com.myweb.backend.service;

import com.myweb.backend.common.ApiException;
import com.myweb.backend.config.FileStorageProperties;
import com.myweb.backend.repository.ManagedFileRepository;
import com.myweb.backend.storage.FileStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FileManagementServiceValidationTest {

    @Mock
    private ManagedFileRepository managedFileRepository;
    @Mock
    private FileStoragePort fileStorage;

    private FileManagementService fileManagementService;

    @BeforeEach
    void setUp() {
        FileStorageProperties props = new FileStorageProperties(
                "",
                1024,
                false,
                new FileStorageProperties.MinioProperties("http://localhost", "a", "b", "c", "")
        );
        fileManagementService = new FileManagementService(managedFileRepository, fileStorage, props);
    }

    @Test
    void acceptsNullOrBlankListFileType() {
        assertThatCode(() -> fileManagementService.validateListFileType(null)).doesNotThrowAnyException();
        assertThatCode(() -> fileManagementService.validateListFileType("  ")).doesNotThrowAnyException();
    }

    @Test
    void rejectsMalformedFileType() {
        assertThatThrownBy(() -> fileManagementService.validateListFileType("not-a-mime"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void acceptsWellFormedMime() {
        assertThatCode(() -> fileManagementService.validateListFileType("image/png")).doesNotThrowAnyException();
    }
}
