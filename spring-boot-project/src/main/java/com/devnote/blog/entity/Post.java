package com.devnote.blog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 엔티티
 * 
 * 블로그의 핵심 콘텐츠인 게시글 정보를 관리합니다.
 * 제목, 내용, 상태, 조회수, 좋아요 수 등의 정보를 포함하며,
 * 작성자, 카테고리, 태그와의 연관관계를 가집니다.
 * 
 * @author DevNote Team
 * @version 1.0.0
 */
@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_slug", columnList = "slug"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_published_at", columnList = "publishedAt"),
    @Index(name = "idx_author_id", columnList = "author_id"),
    @Index(name = "idx_category_id", columnList = "category_id"),
    @Index(name = "idx_is_featured", columnList = "isFeatured"),
    @Index(name = "idx_view_count", columnList = "viewCount")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "featured_image")
    private String featuredImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Column(length = 255)
    private String password;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "comment_count")
    @Builder.Default
    private Integer commentCount = 0;

    @Column(name = "reading_time")
    @Builder.Default
    private Integer readingTime = 0; // 분 단위

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "allow_comments")
    @Builder.Default
    private Boolean allowComments = true;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 연관관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "post_tags",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Like> likes = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<File> files = new ArrayList<>();

    /**
     * 게시글 상태 열거형
     */
    public enum Status {
        DRAFT,      // 임시저장
        PUBLISHED,  // 발행
        ARCHIVED    // 보관
    }

    /**
     * 게시글 공개 범위 열거형
     */
    public enum Visibility {
        PUBLIC,                 // 공개
        PRIVATE,               // 비공개
        PASSWORD_PROTECTED     // 비밀번호 보호
    }

    /**
     * 게시글 발행
     */
    public void publish() {
        this.status = Status.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    /**
     * 게시글 임시저장
     */
    public void draft() {
        this.status = Status.DRAFT;
    }

    /**
     * 게시글 보관
     */
    public void archive() {
        this.status = Status.ARCHIVED;
    }

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 좋아요 수 증가
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수 감소
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 댓글 수 증가
     */
    public void incrementCommentCount() {
        this.commentCount++;
    }

    /**
     * 댓글 수 감소
     */
    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    /**
     * 태그 추가
     */
    public void addTag(Tag tag) {
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
            tag.incrementPostCount();
        }
    }

    /**
     * 태그 제거
     */
    public void removeTag(Tag tag) {
        if (this.tags.remove(tag)) {
            tag.decrementPostCount();
        }
    }

    /**
     * 발행된 게시글 여부 확인
     */
    public boolean isPublished() {
        return this.status == Status.PUBLISHED;
    }

    /**
     * 공개 게시글 여부 확인
     */
    public boolean isPublic() {
        return this.visibility == Visibility.PUBLIC;
    }

    /**
     * 댓글 허용 여부 확인
     */
    public boolean isCommentAllowed() {
        return this.allowComments && this.isPublished();
    }

    /**
     * 읽기 시간 계산 (분 단위)
     * 평균 읽기 속도: 200 단어/분
     */
    public void calculateReadingTime() {
        if (this.content != null) {
            int wordCount = this.content.split("\\s+").length;
            this.readingTime = Math.max(1, (int) Math.ceil(wordCount / 200.0));
        }
    }
}
