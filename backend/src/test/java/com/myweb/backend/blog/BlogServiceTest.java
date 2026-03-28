package com.myweb.backend.blog;

import com.myweb.backend.common.ApiException;
import com.myweb.backend.common.BlogStatus;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.dto.BlogCreateRequest;
import com.myweb.backend.dto.BlogResponseDTO;
import com.myweb.backend.dto.BlogUpdateRequest;
import com.myweb.backend.service.BlogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** BlogService：公开列表过滤草稿、发布后可见。 */
@SpringBootTest
@Transactional
class BlogServiceTest {
    @Autowired
    private BlogService blogService;

    @Test
    void shouldHideDraftFromPublicListUntilPublished() {
        BlogResponseDTO created = blogService.create(new BlogCreateRequest(
                "Post",
                "my-post",
                "summary",
                "cat",
                List.of("t1"),
                "content here",
                null
        ));
        assertThat(created.status()).isEqualTo(BlogStatus.DRAFT);

        PagedResult<BlogResponseDTO> before = blogService.listPublic(null, null, 0, 10);
        assertThat(before.list().stream().noneMatch(b -> b.id().equals(created.id()))).isTrue();

        blogService.publish(created.id());
        PagedResult<BlogResponseDTO> after = blogService.listPublic(null, null, 0, 10);
        assertThat(after.list().stream().anyMatch(b -> b.id().equals(created.id()))).isTrue();

        blogService.unpublish(created.id());
        PagedResult<BlogResponseDTO> afterUnpublish = blogService.listPublic(null, null, 0, 10);
        assertThat(afterUnpublish.list().stream().noneMatch(b -> b.id().equals(created.id()))).isTrue();
    }

    @Test
    void shouldNormalizeSlugToLowerCase() {
        BlogResponseDTO created = blogService.create(new BlogCreateRequest(
                "T",
                "UPPER-Slug",
                "s",
                null,
                List.of("a"),
                "c",
                null
        ));
        assertThat(created.slug()).isEqualTo("upper-slug");
    }

    @Test
    void shouldThrowWhenUpdatingToTakenSlug() {
        BlogResponseDTO a = blogService.create(new BlogCreateRequest(
                "A", "slug-a", "s", null, List.of("x"), "c", null));
        blogService.create(new BlogCreateRequest(
                "B", "slug-b", "s", null, List.of("x"), "c", null));

        assertThatThrownBy(() -> blogService.update(a.id(), new BlogUpdateRequest(
                "A", "slug-b", "s", null, List.of("x"), "c", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo("VALIDATION_ERROR"));
    }
}
