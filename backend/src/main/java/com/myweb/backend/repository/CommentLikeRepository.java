package com.myweb.backend.repository;

import com.myweb.backend.entity.CommentLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 点赞幂等：同一用户对同一评论至多一行。 */
public interface CommentLikeRepository extends JpaRepository<CommentLikeEntity, Long> {
    Optional<CommentLikeEntity> findByCommentIdAndUserId(Long commentId, Long userId);

    void deleteByCommentIdAndUserId(Long commentId, Long userId);
}
