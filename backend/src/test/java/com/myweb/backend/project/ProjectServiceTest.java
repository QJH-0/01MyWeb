package com.myweb.backend.project;

import com.myweb.backend.common.PagedResult;
import com.myweb.backend.common.ApiException;
import com.myweb.backend.dto.ProjectCreateRequest;
import com.myweb.backend.dto.ProjectResponseDTO;
import com.myweb.backend.dto.ProjectUpdateRequest;
import com.myweb.backend.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** ProjectService 事务内行为：创建、更新、标签同步与边界异常。 */
@SpringBootTest
@Transactional
class ProjectServiceTest {
    @Autowired
    private ProjectService projectService;

    @Test
    void shouldFilterInvisibleFromPublicList() {
        ProjectResponseDTO created = projectService.create(new ProjectCreateRequest(
                "P1",
                "S1",
                null,
                "web",
                List.of("Vue"),
                null,
                null,
                null,
                0,
                true
        ));

        PagedResult<ProjectResponseDTO> publicList = projectService.listPublic(null, 0, 10);
        assertThat(publicList.total()).isGreaterThanOrEqualTo(1);
        assertThat(publicList.list().stream().anyMatch(p -> p.id().equals(created.id()))).isTrue();

        projectService.update(created.id(), new ProjectUpdateRequest(
                "P1",
                "S1",
                null,
                "web",
                List.of("Vue"),
                null,
                null,
                null,
                0,
                false
        ));

        PagedResult<ProjectResponseDTO> publicListAfterHide = projectService.listPublic(null, 0, 10);
        assertThat(publicListAfterHide.list().stream().anyMatch(p -> p.id().equals(created.id()))).isFalse();

        assertThatThrownBy(() -> projectService.getPublic(created.id()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo("NOT_FOUND"));
    }
}

