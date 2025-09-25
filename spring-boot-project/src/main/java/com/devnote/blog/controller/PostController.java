package com.devnote.blog.controller;

import com.devnote.blog.dto.PostCreateRequest;
import com.devnote.blog.dto.PostResponse;
import com.devnote.blog.dto.PostUpdateRequest;
import com.devnote.blog.dto.SearchRequest;
import com.devnote.blog.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 게시글 컨트롤러
 * 
 * 게시글 관련 API를 제공합니다.
 * - 게시글 CRUD 작업
 * - 검색 및 필터링
 * - 조회수 추적
 * - 인기 게시글 조회
 * 
 * @author DevNote Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "게시글 관리 API")
public class PostController {

    private final PostService postService;

    /**
     * 게시글 목록 조회
     * 
     * @param pageable 페이지네이션 정보
     * @param category 카테고리 필터
     * @param tag 태그 필터
     * @param search 검색 키워드
     * @param sort 정렬 기준
     * @return 게시글 목록
     */
    @GetMapping
    @Operation(
        summary = "게시글 목록 조회",
        description = "게시글 목록을 조회합니다. 다양한 필터링 옵션을 제공합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<Page<PostResponse>> getPosts(
            @PageableDefault(size = 10) Pageable pageable,
            @Parameter(description = "카테고리 슬러그") @RequestParam(required = false) String category,
            @Parameter(description = "태그 슬러그") @RequestParam(required = false) String tag,
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String search,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "latest") String sort) {
        
        log.info("게시글 목록 조회 요청 - page: {}, category: {}, tag: {}, search: {}, sort: {}", 
                pageable.getPageNumber(), category, tag, search, sort);
        
        Page<PostResponse> posts = postService.getPosts(pageable, category, tag, search, sort);
        return ResponseEntity.ok(posts);
    }

    /**
     * 게시글 상세 조회
     * 
     * @param id 게시글 ID
     * @return 게시글 상세 정보
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "게시글 상세 조회",
        description = "특정 게시글의 상세 정보를 조회합니다. 조회수도 증가합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "게시글 조회 성공"),
        @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    public ResponseEntity<PostResponse> getPost(
            @Parameter(description = "게시글 ID") @PathVariable Long id) {
        
        log.info("게시글 상세 조회 요청 - id: {}", id);
        
        PostResponse post = postService.getPost(id);
        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 작성
     * 
     * @param request 게시글 작성 요청
     * @return 생성된 게시글 정보
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "게시글 작성",
        description = "새로운 게시글을 작성합니다. 관리자 권한이 필요합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "게시글 작성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostCreateRequest request) {
        
        log.info("게시글 작성 요청 - title: {}", request.getTitle());
        
        PostResponse post = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    /**
     * 게시글 수정
     * 
     * @param id 게시글 ID
     * @param request 게시글 수정 요청
     * @return 수정된 게시글 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "게시글 수정",
        description = "기존 게시글을 수정합니다. 작성자 또는 관리자만 가능합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "게시글 수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    public ResponseEntity<PostResponse> updatePost(
            @Parameter(description = "게시글 ID") @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest request) {
        
        log.info("게시글 수정 요청 - id: {}, title: {}", id, request.getTitle());
        
        PostResponse post = postService.updatePost(id, request);
        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 삭제
     * 
     * @param id 게시글 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "게시글 삭제",
        description = "게시글을 삭제합니다. 작성자 또는 관리자만 가능합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "게시글 삭제 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "게시글 ID") @PathVariable Long id) {
        
        log.info("게시글 삭제 요청 - id: {}", id);
        
        postService.deletePost(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 게시글 검색
     * 
     * @param request 검색 요청
     * @param pageable 페이지네이션 정보
     * @return 검색 결과
     */
    @GetMapping("/search")
    @Operation(
        summary = "게시글 검색",
        description = "게시글을 검색합니다. 제목, 내용, 태그에서 키워드를 검색합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "검색 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @Valid SearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        
        log.info("게시글 검색 요청 - query: {}, page: {}", request.getQuery(), pageable.getPageNumber());
        
        Page<PostResponse> posts = postService.searchPosts(request, pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * 인기 게시글 조회
     * 
     * @param pageable 페이지네이션 정보
     * @return 인기 게시글 목록
     */
    @GetMapping("/popular")
    @Operation(
        summary = "인기 게시글 조회",
        description = "조회수와 좋아요 수를 기준으로 인기 게시글을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "인기 게시글 조회 성공")
    })
    public ResponseEntity<Page<PostResponse>> getPopularPosts(
            @PageableDefault(size = 10) Pageable pageable) {
        
        log.info("인기 게시글 조회 요청 - page: {}", pageable.getPageNumber());
        
        Page<PostResponse> posts = postService.getPopularPosts(pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * 피처드 게시글 조회
     * 
     * @param pageable 페이지네이션 정보
     * @return 피처드 게시글 목록
     */
    @GetMapping("/featured")
    @Operation(
        summary = "피처드 게시글 조회",
        description = "피처드로 설정된 게시글을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "피처드 게시글 조회 성공")
    })
    public ResponseEntity<Page<PostResponse>> getFeaturedPosts(
            @PageableDefault(size = 10) Pageable pageable) {
        
        log.info("피처드 게시글 조회 요청 - page: {}", pageable.getPageNumber());
        
        Page<PostResponse> posts = postService.getFeaturedPosts(pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * 게시글 조회수 증가
     * 
     * @param id 게시글 ID
     * @return 조회수 증가 결과
     */
    @PostMapping("/{id}/view")
    @Operation(
        summary = "게시글 조회수 증가",
        description = "게시글의 조회수를 1 증가시킵니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회수 증가 성공"),
        @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    public ResponseEntity<Void> incrementViewCount(
            @Parameter(description = "게시글 ID") @PathVariable Long id) {
        
        log.info("게시글 조회수 증가 요청 - id: {}", id);
        
        postService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }
}
