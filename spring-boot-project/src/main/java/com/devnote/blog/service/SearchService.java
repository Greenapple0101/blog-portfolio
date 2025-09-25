package com.devnote.blog.service;

import com.devnote.blog.dto.PostResponse;
import com.devnote.blog.entity.Post;
import com.devnote.blog.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 검색 서비스
 * 
 * 게시글 검색 기능을 제공합니다.
 * - Full-text search
 * - 하이라이팅
 * - 검색 결과 순위화
 * - 검색 인덱스 관리
 * 
 * @author DevNote Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final PostRepository postRepository;
    
    // 메모리 기반 검색 인덱스 (실제로는 Elasticsearch나 Solr 사용)
    private final Map<Long, SearchIndex> searchIndex = new ConcurrentHashMap<>();

    /**
     * 게시글 검색
     * 
     * @param query 검색 쿼리
     * @param pageable 페이지네이션 정보
     * @return 검색 결과
     */
    @Cacheable(value = "search", key = "#query + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<PostResponse> searchPosts(String query, Pageable pageable) {
        log.info("게시글 검색 - query: {}, page: {}", query, pageable.getPageNumber());

        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("검색 쿼리가 비어있습니다");
        }

        // 검색 쿼리 정규화
        String normalizedQuery = normalizeQuery(query);
        
        // 검색 실행
        List<SearchResult> results = performSearch(normalizedQuery);
        
        // 페이지네이션 적용
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), results.size());
        List<SearchResult> pagedResults = results.subList(start, end);
        
        // PostResponse로 변환
        List<PostResponse> postResponses = pagedResults.stream()
                .map(this::convertToPostResponse)
                .toList();

        // Page 객체 생성
        return new org.springframework.data.domain.PageImpl<>(
                postResponses, 
                pageable, 
                results.size()
        );
    }

    /**
     * 게시글을 검색 인덱스에 추가
     * 
     * @param post 게시글
     */
    @Transactional
    public void indexPost(Post post) {
        log.info("게시글 인덱싱 - id: {}, title: {}", post.getId(), post.getTitle());

        SearchIndex index = SearchIndex.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .excerpt(post.getExcerpt())
                .tags(post.getTags().stream()
                        .map(tag -> tag.getName())
                        .toList())
                .build();

        searchIndex.put(post.getId(), index);
        
        log.info("게시글 인덱싱 완료 - id: {}", post.getId());
    }

    /**
     * 게시글을 검색 인덱스에서 제거
     * 
     * @param postId 게시글 ID
     */
    @Transactional
    public void removePostFromIndex(Long postId) {
        log.info("게시글 인덱스 제거 - id: {}", postId);

        searchIndex.remove(postId);
        
        log.info("게시글 인덱스 제거 완료 - id: {}", postId);
    }

    /**
     * 검색 실행
     * 
     * @param query 검색 쿼리
     * @return 검색 결과
     */
    private List<SearchResult> performSearch(String query) {
        List<SearchResult> results = searchIndex.values().stream()
                .map(index -> calculateRelevance(index, query))
                .filter(result -> result.getScore() > 0)
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .toList();

        log.info("검색 완료 - query: {}, results: {}", query, results.size());
        return results;
    }

    /**
     * 관련도 계산
     * 
     * @param index 검색 인덱스
     * @param query 검색 쿼리
     * @return 검색 결과
     */
    private SearchResult calculateRelevance(SearchIndex index, String query) {
        double score = 0.0;
        String highlightedTitle = index.getTitle();
        String highlightedContent = index.getContent();

        // 제목에서 검색 (가중치: 3.0)
        if (index.getTitle().toLowerCase().contains(query.toLowerCase())) {
            score += 3.0;
            highlightedTitle = highlightText(index.getTitle(), query);
        }

        // 내용에서 검색 (가중치: 1.0)
        if (index.getContent().toLowerCase().contains(query.toLowerCase())) {
            score += 1.0;
            highlightedContent = highlightText(index.getContent(), query);
        }

        // 태그에서 검색 (가중치: 2.0)
        for (String tag : index.getTags()) {
            if (tag.toLowerCase().contains(query.toLowerCase())) {
                score += 2.0;
                break;
            }
        }

        return SearchResult.builder()
                .id(index.getId())
                .title(index.getTitle())
                .content(index.getContent())
                .excerpt(index.getExcerpt())
                .highlightedTitle(highlightedTitle)
                .highlightedContent(highlightedContent)
                .score(score)
                .build();
    }

    /**
     * 텍스트 하이라이팅
     * 
     * @param text 원본 텍스트
     * @param query 검색 쿼리
     * @return 하이라이팅된 텍스트
     */
    private String highlightText(String text, String query) {
        if (text == null || query == null) {
            return text;
        }

        return text.replaceAll("(?i)(" + query + ")", "<mark>$1</mark>");
    }

    /**
     * 검색 쿼리 정규화
     * 
     * @param query 원본 쿼리
     * @return 정규화된 쿼리
     */
    private String normalizeQuery(String query) {
        return query.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    /**
     * SearchResult를 PostResponse로 변환
     * 
     * @param result 검색 결과
     * @return 게시글 응답 DTO
     */
    private PostResponse convertToPostResponse(SearchResult result) {
        // 실제로는 데이터베이스에서 게시글 정보를 조회
        Post post = postRepository.findById(result.getId())
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다: " + result.getId()));

        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .excerpt(post.getExcerpt())
                .content(post.getContent())
                .featuredImage(post.getFeatureImage())
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
                .highlightedTitle(result.getHighlightedTitle())
                .highlightedContent(result.getHighlightedContent())
                .searchScore(result.getScore())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    /**
     * 검색 인덱스 데이터 클래스
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class SearchIndex {
        private Long id;
        private String title;
        private String content;
        private String excerpt;
        private List<String> tags;
    }

    /**
     * 검색 결과 데이터 클래스
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class SearchResult {
        private Long id;
        private String title;
        private String content;
        private String excerpt;
        private String highlightedTitle;
        private String highlightedContent;
        private double score;
    }
}
