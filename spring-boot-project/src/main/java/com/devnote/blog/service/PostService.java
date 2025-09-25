package com.devnote.blog.service;

import com.devnote.blog.dto.PostCreateRequest;
import com.devnote.blog.dto.PostResponse;
import com.devnote.blog.dto.PostUpdateRequest;
import com.devnote.blog.dto.SearchRequest;
import com.devnote.blog.entity.Post;
import com.devnote.blog.entity.User;
import com.devnote.blog.repository.PostRepository;
import com.devnote.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시글 서비스
 * 
 * 게시글 관련 비즈니스 로직을 처리합니다.
 * - 게시글 CRUD 작업
 * - 검색 및 필터링
 * - 조회수 추적
 * - 캐싱 처리
 * 
 * @author DevNote Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SearchService searchService;
    private final CacheService cacheService;

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
    @Cacheable(value = "posts", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #category + '_' + #tag + '_' + #search + '_' + #sort")
    public Page<PostResponse> getPosts(Pageable pageable, String category, String tag, String search, String sort) {
        log.info("게시글 목록 조회 - page: {}, category: {}, tag: {}, search: {}, sort: {}", 
                pageable.getPageNumber(), category, tag, search, sort);

        // 검색 키워드가 있는 경우 검색 서비스 사용
        if (search != null && !search.trim().isEmpty()) {
            return searchService.searchPosts(search, pageable);
        }

        // 일반 목록 조회
        Page<Post> posts = postRepository.findPublishedPostsWithFilters(category, tag, sort, pageable);
        return posts.map(this::convertToResponse);
    }

    /**
     * 게시글 상세 조회
     * 
     * @param id 게시글 ID
     * @return 게시글 상세 정보
     */
    @Cacheable(value = "post", key = "#id")
    public PostResponse getPost(Long id) {
        log.info("게시글 상세 조회 - id: {}", id);

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다: " + id));

        // 조회수 증가 (비동기 처리)
        incrementViewCountAsync(id);

        return convertToResponse(post);
    }

    /**
     * 게시글 작성
     * 
     * @param request 게시글 작성 요청
     * @return 생성된 게시글 정보
     */
    @Transactional
    @CacheEvict(value = {"posts", "post"}, allEntries = true)
    public PostResponse createPost(PostCreateRequest request) {
        log.info("게시글 작성 - title: {}", request.getTitle());

        // 현재 사용자 조회 (실제로는 SecurityContext에서 가져옴)
        User author = userRepository.findById(1L) // 임시로 관리자 ID 사용
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        Post post = Post.builder()
                .title(request.getTitle())
                .slug(generateSlug(request.getTitle()))
                .excerpt(request.getExcerpt())
                .content(request.getContent())
                .featuredImage(request.getFeaturedImage())
                .status(request.getStatus())
                .isFeatured(request.getIsFeatured())
                .author(author)
                .build();

        // 읽기 시간 계산
        post.calculateReadingTime();

        Post savedPost = postRepository.save(post);

        // 검색 인덱스 업데이트
        searchService.indexPost(savedPost);

        log.info("게시글 작성 완료 - id: {}, title: {}", savedPost.getId(), savedPost.getTitle());
        return convertToResponse(savedPost);
    }

    /**
     * 게시글 수정
     * 
     * @param id 게시글 ID
     * @param request 게시글 수정 요청
     * @return 수정된 게시글 정보
     */
    @Transactional
    @CacheEvict(value = {"posts", "post"}, allEntries = true)
    public PostResponse updatePost(Long id, PostUpdateRequest request) {
        log.info("게시글 수정 - id: {}, title: {}", id, request.getTitle());

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다: " + id));

        // 권한 확인 (실제로는 SecurityContext에서 현재 사용자와 비교)
        if (!post.getAuthor().getId().equals(1L)) { // 임시로 관리자 ID 사용
            throw new RuntimeException("게시글을 수정할 권한이 없습니다");
        }

        // 게시글 정보 업데이트
        post.setTitle(request.getTitle());
        post.setSlug(generateSlug(request.getTitle()));
        post.setExcerpt(request.getExcerpt());
        post.setContent(request.getContent());
        post.setFeaturedImage(request.getFeaturedImage());
        post.setStatus(request.getStatus());
        post.setIsFeatured(request.getIsFeatured());

        // 읽기 시간 재계산
        post.calculateReadingTime();

        Post updatedPost = postRepository.save(post);

        // 검색 인덱스 업데이트
        searchService.indexPost(updatedPost);

        log.info("게시글 수정 완료 - id: {}, title: {}", updatedPost.getId(), updatedPost.getTitle());
        return convertToResponse(updatedPost);
    }

    /**
     * 게시글 삭제
     * 
     * @param id 게시글 ID
     */
    @Transactional
    @CacheEvict(value = {"posts", "post"}, allEntries = true)
    public void deletePost(Long id) {
        log.info("게시글 삭제 - id: {}", id);

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다: " + id));

        // 권한 확인
        if (!post.getAuthor().getId().equals(1L)) { // 임시로 관리자 ID 사용
            throw new RuntimeException("게시글을 삭제할 권한이 없습니다");
        }

        // 검색 인덱스에서 제거
        searchService.removePostFromIndex(id);

        postRepository.delete(post);

        log.info("게시글 삭제 완료 - id: {}", id);
    }

    /**
     * 게시글 검색
     * 
     * @param request 검색 요청
     * @param pageable 페이지네이션 정보
     * @return 검색 결과
     */
    public Page<PostResponse> searchPosts(SearchRequest request, Pageable pageable) {
        log.info("게시글 검색 - query: {}, page: {}", request.getQuery(), pageable.getPageNumber());

        return searchService.searchPosts(request.getQuery(), pageable);
    }

    /**
     * 인기 게시글 조회
     * 
     * @param pageable 페이지네이션 정보
     * @return 인기 게시글 목록
     */
    @Cacheable(value = "popularPosts", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<PostResponse> getPopularPosts(Pageable pageable) {
        log.info("인기 게시글 조회 - page: {}", pageable.getPageNumber());

        Page<Post> posts = postRepository.findPopularPosts(pageable);
        return posts.map(this::convertToResponse);
    }

    /**
     * 피처드 게시글 조회
     * 
     * @param pageable 페이지네이션 정보
     * @return 피처드 게시글 목록
     */
    @Cacheable(value = "featuredPosts", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<PostResponse> getFeaturedPosts(Pageable pageable) {
        log.info("피처드 게시글 조회 - page: {}", pageable.getPageNumber());

        Page<Post> posts = postRepository.findFeaturedPosts(pageable);
        return posts.map(this::convertToResponse);
    }

    /**
     * 조회수 증가
     * 
     * @param id 게시글 ID
     */
    @Transactional
    public void incrementViewCount(Long id) {
        log.info("조회수 증가 - id: {}", id);

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다: " + id));

        post.incrementViewCount();
        postRepository.save(post);

        // 캐시 무효화
        cacheService.evictCache("post", id.toString());
    }

    /**
     * 조회수 증가 (비동기)
     * 
     * @param id 게시글 ID
     */
    public void incrementViewCountAsync(Long id) {
        // 실제로는 @Async를 사용하여 비동기 처리
        incrementViewCount(id);
    }

    /**
     * 게시글을 응답 DTO로 변환
     * 
     * @param post 게시글 엔티티
     * @return 게시글 응답 DTO
     */
    private PostResponse convertToResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .excerpt(post.getExcerpt())
                .content(post.getContent())
                .featuredImage(post.getFeaturedImage())
                .status(post.getStatus())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .readingTime(post.getReadingTime())
                .isFeatured(post.getIsFeatured())
                .publishedAt(post.getPublishedAt())
                .authorName(post.getAuthor().getUsername())
                .authorProfileImage(post.getAuthor().getProfileImage())
                .categoryName(post.getCategory() != null ? post.getCategory().getName() : null)
                .categorySlug(post.getCategory() != null ? post.getCategory().getSlug() : null)
                .tags(post.getTags().stream()
                        .map(tag -> PostResponse.TagResponse.builder()
                                .id(tag.getId())
                                .name(tag.getName())
                                .slug(tag.getSlug())
                                .color(tag.getColor())
                                .build())
                        .toList())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    /**
     * 슬러그 생성
     * 
     * @param title 제목
     * @return 생성된 슬러그
     */
    private String generateSlug(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("제목이 비어있습니다");
        }

        String slug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();

        // 중복 확인 및 번호 추가
        String originalSlug = slug;
        int counter = 1;
        while (postRepository.existsBySlug(slug)) {
            slug = originalSlug + "-" + counter;
            counter++;
        }

        return slug;
    }
}
