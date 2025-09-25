package com.devnote.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DevNote Blog Application
 * 
 * 백엔드 개발자가 직접 설계하고 구현한 블로그 시스템
 * 
 * 주요 기능:
 * - JWT 기반 인증/인가
 * - 게시글 CRUD 및 검색
 * - 댓글 시스템 (계층형)
 * - 통계 및 분석
 * - 파일 업로드
 * - 캐싱 및 성능 최적화
 * 
 * 기술 스택:
 * - Spring Boot 3.x
 * - Spring Security
 * - Spring Data JPA
 * - MySQL 8.0
 * - Redis
 * - Swagger/OpenAPI 3
 * 
 * @author DevNote Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class DevNoteBlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevNoteBlogApplication.class, args);
    }
}
