-- DevNote 블로그 시스템 데이터베이스 설계
-- MySQL 8.0 기준

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS devnote CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE devnote;

-- 1. 사용자 테이블
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'USER') DEFAULT 'USER',
    profile_image VARCHAR(255),
    bio TEXT,
    github_url VARCHAR(255),
    linkedin_url VARCHAR(255),
    website_url VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_created_at (created_at)
);

-- 2. 카테고리 테이블
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    color VARCHAR(7) DEFAULT '#007bff', -- HEX 색상 코드
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_slug (slug),
    INDEX idx_sort_order (sort_order)
);

-- 3. 태그 테이블
CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    slug VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    color VARCHAR(7) DEFAULT '#6c757d',
    post_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_slug (slug),
    INDEX idx_post_count (post_count)
);

-- 4. 게시글 테이블
CREATE TABLE posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    excerpt TEXT,
    content LONGTEXT NOT NULL,
    featured_image VARCHAR(255),
    status ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED') DEFAULT 'DRAFT',
    visibility ENUM('PUBLIC', 'PRIVATE', 'PASSWORD_PROTECTED') DEFAULT 'PUBLIC',
    password VARCHAR(255) NULL,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    reading_time INT DEFAULT 0, -- 분 단위
    is_featured BOOLEAN DEFAULT FALSE,
    allow_comments BOOLEAN DEFAULT TRUE,
    published_at TIMESTAMP NULL,
    author_id BIGINT NOT NULL,
    category_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    
    INDEX idx_slug (slug),
    INDEX idx_status (status),
    INDEX idx_published_at (published_at),
    INDEX idx_author_id (author_id),
    INDEX idx_category_id (category_id),
    INDEX idx_is_featured (is_featured),
    INDEX idx_view_count (view_count),
    FULLTEXT idx_title_content (title, content, excerpt)
);

-- 5. 게시글-태그 연결 테이블 (다대다 관계)
CREATE TABLE post_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    
    UNIQUE KEY unique_post_tag (post_id, tag_id),
    INDEX idx_post_id (post_id),
    INDEX idx_tag_id (tag_id)
);

-- 6. 댓글 테이블 (계층형 구조)
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    author_email VARCHAR(100) NOT NULL,
    author_website VARCHAR(255),
    author_ip VARCHAR(45), -- IPv6 지원
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'SPAM') DEFAULT 'PENDING',
    is_admin BOOLEAN DEFAULT FALSE,
    like_count INT DEFAULT 0,
    parent_id BIGINT NULL, -- 대댓글을 위한 자기참조
    post_id BIGINT NOT NULL,
    user_id BIGINT NULL, -- 로그인한 사용자의 경우
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_post_id (post_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- 7. 파일 업로드 테이블
CREATE TABLE files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_type ENUM('IMAGE', 'DOCUMENT', 'VIDEO', 'AUDIO', 'OTHER') NOT NULL,
    uploader_id BIGINT NOT NULL,
    post_id BIGINT NULL, -- 게시글에 첨부된 파일인 경우
    is_public BOOLEAN DEFAULT TRUE,
    download_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE SET NULL,
    
    INDEX idx_uploader_id (uploader_id),
    INDEX idx_post_id (post_id),
    INDEX idx_file_type (file_type),
    INDEX idx_created_at (created_at)
);

-- 8. 방문자 통계 테이블
CREATE TABLE analytics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent TEXT,
    referer VARCHAR(500),
    page_url VARCHAR(500) NOT NULL,
    page_title VARCHAR(255),
    post_id BIGINT NULL,
    user_id BIGINT NULL,
    country VARCHAR(100),
    city VARCHAR(100),
    device_type ENUM('DESKTOP', 'MOBILE', 'TABLET') DEFAULT 'DESKTOP',
    browser VARCHAR(100),
    os VARCHAR(100),
    visit_duration INT DEFAULT 0, -- 초 단위
    is_bounce BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_session_id (session_id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_post_id (post_id),
    INDEX idx_created_at (created_at),
    INDEX idx_device_type (device_type)
);

-- 9. 좋아요 테이블
CREATE TABLE likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NULL,
    comment_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    
    UNIQUE KEY unique_user_post_like (user_id, post_id),
    UNIQUE KEY unique_user_comment_like (user_id, comment_id),
    INDEX idx_user_id (user_id),
    INDEX idx_post_id (post_id),
    INDEX idx_comment_id (comment_id)
);

-- 10. 시스템 설정 테이블
CREATE TABLE settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    description TEXT,
    setting_type ENUM('STRING', 'NUMBER', 'BOOLEAN', 'JSON') DEFAULT 'STRING',
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_setting_key (setting_key),
    INDEX idx_is_public (is_public)
);

-- 11. 알림 테이블
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type ENUM('INFO', 'WARNING', 'ERROR', 'SUCCESS') DEFAULT 'INFO',
    is_read BOOLEAN DEFAULT FALSE,
    related_id BIGINT NULL, -- 관련 게시글, 댓글 등의 ID
    related_type VARCHAR(50) NULL, -- POST, COMMENT 등
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
);

-- 초기 데이터 삽입

-- 기본 카테고리
INSERT INTO categories (name, slug, description, color, sort_order) VALUES
('기술', 'tech', '개발 관련 기술 글', '#007bff', 1),
('프로젝트', 'project', '프로젝트 소개 및 후기', '#28a745', 2),
('일상', 'daily', '일상적인 이야기', '#ffc107', 3),
('리뷰', 'review', '책, 영화, 서비스 리뷰', '#dc3545', 4);

-- 기본 태그
INSERT INTO tags (name, slug, description, color) VALUES
('Spring Boot', 'spring-boot', 'Spring Boot 관련', '#6db33f'),
('Java', 'java', 'Java 프로그래밍', '#f89820'),
('Database', 'database', '데이터베이스 관련', '#336791'),
('API', 'api', 'API 설계 및 개발', '#61dafb'),
('Docker', 'docker', 'Docker 컨테이너', '#2496ed'),
('AWS', 'aws', 'Amazon Web Services', '#ff9900');

-- 기본 설정
INSERT INTO settings (setting_key, setting_value, description, setting_type, is_public) VALUES
('site_title', 'DevNote', '사이트 제목', 'STRING', TRUE),
('site_description', '개발자의 기술 블로그', '사이트 설명', 'STRING', TRUE),
('posts_per_page', '10', '페이지당 게시글 수', 'NUMBER', TRUE),
('allow_comments', 'true', '댓글 허용 여부', 'BOOLEAN', TRUE),
('require_comment_approval', 'false', '댓글 승인 필요 여부', 'BOOLEAN', FALSE),
('maintenance_mode', 'false', '점검 모드', 'BOOLEAN', FALSE);

-- 관리자 계정 생성 (비밀번호: admin123)
INSERT INTO users (username, email, password, role, bio, is_active, email_verified) VALUES
('admin', 'admin@devnote.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'ADMIN', 'DevNote 관리자', TRUE, TRUE);

-- 트리거: 게시글 삭제 시 태그 카운트 감소
DELIMITER //
CREATE TRIGGER after_post_delete
AFTER DELETE ON posts
FOR EACH ROW
BEGIN
    UPDATE tags t
    SET post_count = (
        SELECT COUNT(*)
        FROM post_tags pt
        WHERE pt.tag_id = t.id
    )
    WHERE t.id IN (
        SELECT tag_id
        FROM post_tags
        WHERE post_id = OLD.id
    );
END//
DELIMITER ;

-- 트리거: 게시글-태그 연결 시 태그 카운트 증가
DELIMITER //
CREATE TRIGGER after_post_tag_insert
AFTER INSERT ON post_tags
FOR EACH ROW
BEGIN
    UPDATE tags
    SET post_count = post_count + 1
    WHERE id = NEW.tag_id;
END//
DELIMITER ;

-- 트리거: 게시글-태그 연결 삭제 시 태그 카운트 감소
DELIMITER //
CREATE TRIGGER after_post_tag_delete
AFTER DELETE ON post_tags
FOR EACH ROW
BEGIN
    UPDATE tags
    SET post_count = post_count - 1
    WHERE id = OLD.tag_id;
END//
DELIMITER ;

-- 뷰: 게시글 통계
CREATE VIEW post_stats AS
SELECT 
    p.id,
    p.title,
    p.slug,
    p.view_count,
    p.like_count,
    p.comment_count,
    p.published_at,
    u.username as author_name,
    c.name as category_name,
    GROUP_CONCAT(t.name) as tags
FROM posts p
LEFT JOIN users u ON p.author_id = u.id
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN post_tags pt ON p.id = pt.post_id
LEFT JOIN tags t ON pt.tag_id = t.id
WHERE p.status = 'PUBLISHED'
GROUP BY p.id;

-- 뷰: 인기 게시글 (최근 30일 기준)
CREATE VIEW popular_posts AS
SELECT 
    p.id,
    p.title,
    p.slug,
    p.view_count,
    p.like_count,
    p.published_at,
    u.username as author_name,
    c.name as category_name
FROM posts p
LEFT JOIN users u ON p.author_id = u.id
LEFT JOIN categories c ON p.category_id = c.id
WHERE p.status = 'PUBLISHED'
    AND p.published_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY (p.view_count * 0.7 + p.like_count * 0.3) DESC;
