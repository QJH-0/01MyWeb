package com.myweb.backend.controller;

import com.myweb.backend.common.ApiResponse;
import com.myweb.backend.common.PagedResult;
import com.myweb.backend.common.TraceIdFilter;
import com.myweb.backend.dto.CommentCreateRequest;
import com.myweb.backend.dto.CommentItemDTO;
import com.myweb.backend.dto.CommentLikeResultDTO;
import com.myweb.backend.dto.CommentReplyRequest;
import com.myweb.backend.security.AuthenticatedUser;
import com.myweb.backend.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 评论：列表公开；创建/回复/点赞需登录并具备 {@code PERM_COMMENT_WRITE}（由 Security 配置）。 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ApiResponse<PagedResult<CommentItemDTO>> list(
            @RequestParam String targetType,
            @RequestParam long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(commentService.list(targetType, targetId, page, limit), traceId(httpRequest));
    }

    @PostMapping
    public ApiResponse<CommentItemDTO> create(
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(commentService.create(principal.userId(), request), traceId(httpRequest));
    }

    @PostMapping("/{id}/reply")
    public ApiResponse<CommentItemDTO> reply(
            @PathVariable long id,
            @Valid @RequestBody CommentReplyRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(commentService.reply(principal.userId(), id, request), traceId(httpRequest));
    }

    @PostMapping("/{id}/like")
    public ApiResponse<CommentLikeResultDTO> like(
            @PathVariable long id,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(commentService.toggleLike(principal.userId(), id), traceId(httpRequest));
    }

    private String traceId(HttpServletRequest request) {
        Object traceAttr = request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR);
        return traceAttr == null ? "" : traceAttr.toString();
    }
}
