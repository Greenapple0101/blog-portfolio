package com.devnote.blog.service;

import com.devnote.blog.dto.AnalyticsResponse;
import com.devnote.blog.dto.DashboardStatsResponse;
import com.devnote.blog.dto.VisitorStatsResponse;
import com.devnote.blog.entity.Analytics;
import com.devnote.blog.entity.Post;
import com.devnote.blog.repository.AnalyticsRepository;
import com.devnote.blog.repository.PostRepository;
import com.devnote.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 분석 서비스
 * 
 * 블로그 통계 및 분석 기능을 제공합니다.
 * - 방문자 통계
 * - 게시글 통계
 * - 인기 콘텐츠 분석
 * - 사용자 행동 패턴 분석
 * 
 * @author DevNote Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 대시보드 통계 조회
     * 
     * @return 대시보드 통계
     */
    @Cacheable(value = "dashboardStats", key = "'dashboard'")
    public DashboardStatsResponse getDashboardStats() {
        log.info("대시보드 통계 조회");

        // 전체 통계
        long totalPosts = postRepository.count();
        long totalUsers = userRepository.count();
        long totalViews = analyticsRepository.getTotalViews();
        long totalComments = analyticsRepository.getTotalComments();

        // 최근 게시글 (최근 7일)
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<Post> recentPosts = postRepository.findRecentPosts(weekAgo);

        // 인기 게시글 (최근 30일)
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        List<Post> popularPosts = postRepository.findPopularPosts(monthAgo);

        // 방문자 통계 (최근 7일)
        VisitorStatsResponse visitorStats = getVisitorStats("week");

        return DashboardStatsResponse.builder()
                .totalPosts(totalPosts)
                .totalUsers(totalUsers)
                .totalViews(totalViews)
                .totalComments(totalComments)
                .recentPosts(recentPosts.stream()
                        .map(this::convertToPostSummary)
                        .toList())
                .popularPosts(popularPosts.stream()
                        .map(this::convertToPostSummary)
                        .toList())
                .visitorStats(visitorStats)
                .build();
    }

    /**
     * 방문자 통계 조회
     * 
     * @param period 통계 기간 (day, week, month, year)
     * @return 방문자 통계
     */
    @Cacheable(value = "visitorStats", key = "#period")
    public VisitorStatsResponse getVisitorStats(String period) {
        log.info("방문자 통계 조회 - period: {}", period);

        LocalDateTime startDate = getStartDate(period);
        LocalDateTime endDate = LocalDateTime.now();

        // 기본 통계
        long totalVisitors = analyticsRepository.countByCreatedAtBetween(startDate, endDate);
        long uniqueVisitors = analyticsRepository.countDistinctBySessionIdAndCreatedAtBetween(startDate, endDate);
        long pageViews = analyticsRepository.sumPageViewsByCreatedAtBetween(startDate, endDate);
        double averageSessionDuration = analyticsRepository.getAverageSessionDuration(startDate, endDate);
        double bounceRate = analyticsRepository.getBounceRate(startDate, endDate);

        // 인기 페이지
        List<Map<String, Object>> topPages = analyticsRepository.getTopPages(startDate, endDate);

        // 디바이스 통계
        Map<String, Long> deviceStats = analyticsRepository.getDeviceStats(startDate, endDate);

        // 브라우저 통계
        Map<String, Long> browserStats = analyticsRepository.getBrowserStats(startDate, endDate);

        // OS 통계
        Map<String, Long> osStats = analyticsRepository.getOsStats(startDate, endDate);

        // 지역 통계
        Map<String, Long> countryStats = analyticsRepository.getCountryStats(startDate, endDate);

        return VisitorStatsResponse.builder()
                .period(period)
                .totalVisitors(totalVisitors)
                .uniqueVisitors(uniqueVisitors)
                .pageViews(pageViews)
                .averageSessionDuration(averageSessionDuration)
                .bounceRate(bounceRate)
                .topPages(topPages.stream()
                        .map(this::convertToPageStats)
                        .toList())
                .deviceStats(deviceStats)
                .browserStats(browserStats)
                .osStats(osStats)
                .countryStats(countryStats)
                .build();
    }

    /**
     * 게시글 통계 조회
     * 
     * @param period 통계 기간
     * @return 게시글 통계
     */
    @Cacheable(value = "postStats", key = "#period")
    public AnalyticsResponse getPostStats(String period) {
        log.info("게시글 통계 조회 - period: {}", period);

        LocalDateTime startDate = getStartDate(period);
        LocalDateTime endDate = LocalDateTime.now();

        // 게시글별 조회수
        List<Map<String, Object>> postViews = analyticsRepository.getPostViews(startDate, endDate);

        // 카테고리별 통계
        List<Map<String, Object>> categoryStats = analyticsRepository.getCategoryStats(startDate, endDate);

        // 태그별 통계
        List<Map<String, Object>> tagStats = analyticsRepository.getTagStats(startDate, endDate);

        return AnalyticsResponse.builder()
                .period(period)
                .postViews(postViews.stream()
                        .map(this::convertToPostViewStats)
                        .toList())
                .categoryStats(categoryStats.stream()
                        .map(this::convertToCategoryStats)
                        .toList())
                .tagStats(tagStats.stream()
                        .map(this::convertToTagStats)
                        .toList())
                .build();
    }

    /**
     * 실시간 통계 조회
     * 
     * @return 실시간 통계
     */
    @Cacheable(value = "realtimeStats", key = "'realtime'")
    public Map<String, Object> getRealtimeStats() {
        log.info("실시간 통계 조회");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);

        // 현재 온라인 사용자 수
        long onlineUsers = analyticsRepository.countDistinctBySessionIdAndCreatedAtBetween(oneHourAgo, now);

        // 최근 1시간 방문자 수
        long recentVisitors = analyticsRepository.countByCreatedAtBetween(oneHourAgo, now);

        // 최근 1시간 페이지뷰
        long recentPageViews = analyticsRepository.sumPageViewsByCreatedAtBetween(oneHourAgo, now);

        // 인기 페이지 (최근 1시간)
        List<Map<String, Object>> topPages = analyticsRepository.getTopPages(oneHourAgo, now);

        return Map.of(
                "onlineUsers", onlineUsers,
                "recentVisitors", recentVisitors,
                "recentPageViews", recentPageViews,
                "topPages", topPages.stream()
                        .map(this::convertToPageStats)
                        .toList()
        );
    }

    /**
     * 방문자 로그 저장
     * 
     * @param analytics 방문자 분석 데이터
     */
    @Transactional
    public void saveAnalytics(Analytics analytics) {
        log.debug("방문자 로그 저장 - sessionId: {}, pageUrl: {}", 
                analytics.getSessionId(), analytics.getPageUrl());

        analyticsRepository.save(analytics);
    }

    /**
     * 통계 기간에 따른 시작 날짜 계산
     * 
     * @param period 통계 기간
     * @return 시작 날짜
     */
    private LocalDateTime getStartDate(String period) {
        LocalDateTime now = LocalDateTime.now();
        
        return switch (period.toLowerCase()) {
            case "day" -> now.minusDays(1);
            case "week" -> now.minusWeeks(1);
            case "month" -> now.minusMonths(1);
            case "year" -> now.minusYears(1);
            default -> now.minusDays(7);
        };
    }

    /**
     * 게시글을 요약 정보로 변환
     * 
     * @param post 게시글
     * @return 게시글 요약 정보
     */
    private DashboardStatsResponse.PostSummary convertToPostSummary(Post post) {
        return DashboardStatsResponse.PostSummary.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .publishedAt(post.getPublishedAt())
                .build();
    }

    /**
     * 페이지 통계 변환
     * 
     * @param data 페이지 통계 데이터
     * @return 페이지 통계
     */
    private VisitorStatsResponse.PageStats convertToPageStats(Map<String, Object> data) {
        return VisitorStatsResponse.PageStats.builder()
                .page((String) data.get("page"))
                .views(((Number) data.get("views")).longValue())
                .build();
    }

    /**
     * 게시글 조회수 통계 변환
     * 
     * @param data 게시글 조회수 데이터
     * @return 게시글 조회수 통계
     */
    private AnalyticsResponse.PostViewStats convertToPostViewStats(Map<String, Object> data) {
        return AnalyticsResponse.PostViewStats.builder()
                .postId(((Number) data.get("postId")).longValue())
                .title((String) data.get("title"))
                .views(((Number) data.get("views")).longValue())
                .build();
    }

    /**
     * 카테고리 통계 변환
     * 
     * @param data 카테고리 통계 데이터
     * @return 카테고리 통계
     */
    private AnalyticsResponse.CategoryStats convertToCategoryStats(Map<String, Object> data) {
        return AnalyticsResponse.CategoryStats.builder()
                .categoryName((String) data.get("categoryName"))
                .postCount(((Number) data.get("postCount")).longValue())
                .viewCount(((Number) data.get("viewCount")).longValue())
                .build();
    }

    /**
     * 태그 통계 변환
     * 
     * @param data 태그 통계 데이터
     * @return 태그 통계
     */
    private AnalyticsResponse.TagStats convertToTagStats(Map<String, Object> data) {
        return AnalyticsResponse.TagStats.builder()
                .tagName((String) data.get("tagName"))
                .postCount(((Number) data.get("postCount")).longValue())
                .viewCount(((Number) data.get("viewCount")).longValue())
                .build();
    }
}
