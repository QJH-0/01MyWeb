package com.myweb.backend.service;

import com.myweb.backend.common.ApiException;
import com.myweb.backend.common.BlogStatus;
import com.myweb.backend.common.CommentTargetType;
import com.myweb.backend.common.ErrorCodes;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.dto.CommentCreateRequest;
import com.myweb.backend.dto.CommentItemDTO;
import com.myweb.backend.dto.CommentLikeResultDTO;
import com.myweb.backend.dto.CommentReplyRequest;
import com.myweb.backend.entity.CommentEntity;
import com.myweb.backend.entity.CommentLikeEntity;
import com.myweb.backend.entity.UserAccountEntity;
import com.myweb.backend.repository.BlogRepository;
import com.myweb.backend.repository.CommentLikeRepository;
import com.myweb.backend.repository.CommentRepository;
import com.myweb.backend.repository.ProjectRepository;
import com.myweb.backend.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论：公开列表；写操作要求目标对访客可见；点赞在同用户下幂等切换并维护 {@code like_count}。
 */
@Service
public class CommentService {
    private static final int MAX_LIMIT = 100;

    private static final String UNKNOWN_AUTHOR_LABEL = "用户";

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final BlogRepository blogRepository;
    private final ProjectRepository projectRepository;
    private final UserAccountRepository userAccountRepository;

    public CommentService(
            CommentRepository commentRepository,
            CommentLikeRepository commentLikeRepository,
            BlogRepository blogRepository,
            ProjectRepository projectRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.blogRepository = blogRepository;
        this.projectRepository = projectRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public PagedResult<CommentItemDTO> list(String targetTypeRaw, long targetId, int page, int limit) {
        String type = CommentTargetType.normalize(targetTypeRaw);
        assertTargetReadable(type, targetId);
        Pageable pageable = toPageable(page, limit);
        Page<CommentEntity> data = commentRepository.findByTargetTypeAndTargetIdAndDeletedAtIsNull(
                type, targetId, pageable
        );
        List<CommentEntity> entities = data.getContent();
        Map<Long, String> names = resolveUsernames(entities.stream().map(CommentEntity::getAuthorUserId).collect(Collectors.toSet()));
        List<CommentItemDTO> list = entities.stream().map(e -> toDto(e, names)).collect(Collectors.toList());
        return new PagedResult<>(list, data.getTotalElements(), page, limit);
    }

    @Transactional
    public CommentItemDTO create(long authorUserId, CommentCreateRequest request) {
        String type = CommentTargetType.normalize(request.targetType());
        if (!CommentTargetType.isKnown(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Invalid targetType");
        }
        assertTargetReadable(type, request.targetId());
        String text = request.content().trim();
        if (text.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Comment content must not be blank");
        }

        CommentEntity entity = new CommentEntity();
        entity.setTargetType(type);
        entity.setTargetId(request.targetId());
        entity.setParentId(null);
        entity.setAuthorUserId(authorUserId);
        entity.setContent(text);
        entity.setLikeCount(0);
        commentRepository.save(entity);
        return toDto(entity);
    }

    @Transactional
    public CommentItemDTO reply(long authorUserId, long parentCommentId, CommentReplyRequest request) {
        CommentEntity parent = commentRepository.findById(parentCommentId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Parent comment not found"));

        assertTargetReadable(parent.getTargetType(), parent.getTargetId());

        String text = request.content().trim();
        if (text.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Comment content must not be blank");
        }

        CommentEntity entity = new CommentEntity();
        entity.setTargetType(parent.getTargetType());
        entity.setTargetId(parent.getTargetId());
        entity.setParentId(parent.getId());
        entity.setAuthorUserId(authorUserId);
        entity.setContent(text);
        entity.setLikeCount(0);
        commentRepository.save(entity);
        return toDto(entity);
    }

    /** 批量解析用户名，避免列表接口 N+1；账户缺失时兜底展示固定文案「用户」。 */
    private Map<Long, String> resolveUsernames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = userIds.stream().filter(id -> id != null && id > 0).collect(Collectors.toCollection(HashSet::new));
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userAccountRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(UserAccountEntity::getUserId, UserAccountEntity::getUsername));
    }

    @Transactional
    public CommentLikeResultDTO toggleLike(long userId, long commentId) {
        CommentEntity comment = commentRepository.findById(commentId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Comment not found"));

        assertTargetReadable(comment.getTargetType(), comment.getTargetId());

        var existing = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);
        if (existing.isPresent()) {
            commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            int next = Math.max(0, comment.getLikeCount() - 1);
            comment.setLikeCount(next);
            commentRepository.save(comment);
            return new CommentLikeResultDTO(false, next);
        }

        CommentLikeEntity like = new CommentLikeEntity();
        like.setCommentId(commentId);
        like.setUserId(userId);
        commentLikeRepository.save(like);
        int next = comment.getLikeCount() + 1;
        comment.setLikeCount(next);
        commentRepository.save(comment);
        return new CommentLikeResultDTO(true, next);
    }

    private void assertTargetReadable(String normalizedTargetType, long targetId) {
        if (CommentTargetType.BLOG.equals(normalizedTargetType)) {
            blogRepository.findByIdAndDeletedAtIsNullAndStatus(targetId, BlogStatus.PUBLISHED)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Blog not found"));
            return;
        }
        if (CommentTargetType.PROJECT.equals(normalizedTargetType)) {
            projectRepository.findByIdAndDeletedAtIsNullAndVisibleTrue(targetId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, "Project not found"));
            return;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Invalid targetType");
    }

    private Pageable toPageable(int page, int limit) {
        if (page < 0 || limit <= 0 || limit > MAX_LIMIT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Invalid pagination params");
        }
        Sort sort = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
        return PageRequest.of(page, limit, sort);
    }

    private CommentItemDTO toDto(CommentEntity e) {
        return toDto(e, resolveUsernames(Set.of(e.getAuthorUserId())));
    }

    private CommentItemDTO toDto(CommentEntity e, Map<Long, String> usernames) {
        Long uid = e.getAuthorUserId();
        String username = usernames.getOrDefault(uid, UNKNOWN_AUTHOR_LABEL);
        return new CommentItemDTO(
                e.getId(),
                e.getTargetType(),
                e.getTargetId(),
                e.getContent(),
                e.getParentId(),
                uid,
                username,
                e.getLikeCount(),
                e.getCreatedAt()
        );
    }
}
