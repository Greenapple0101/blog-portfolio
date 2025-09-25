# DevNote - 백엔드 중심 블로그 시스템

## 🎯 프로젝트 컨셉

**"개인 블로그를 백엔드부터 직접 설계하고 구현한 서비스"**

- 단순한 글 저장소가 아닌 **백엔드 역량을 보여주는 서비스**
- 아키텍처, API 설계, 보안, 성능 최적화 등 기술 스택 강조
- 실제 운영되는 서비스로 신뢰성 확보

## 🏗 시스템 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   Database      │
│   (React/Vue)   │◄──►│   Spring Boot   │◄──►│   MySQL         │
│                 │    │                 │    │                 │
│ - 블로그 뷰     │    │ - REST API      │    │ - Posts         │
│ - 관리자 페이지 │    │ - JWT 인증      │    │ - Users         │
│ - 통계 대시보드 │    │ - 검색 엔진     │    │ - Tags          │
└─────────────────┘    │ - 캐싱 (Redis)  │    │ - Comments      │
                       │ - 파일 업로드   │    │ - Analytics     │
                       └─────────────────┘    └─────────────────┘
                                │
                       ┌─────────────────┐
                       │   Infrastructure│
                       │                 │
                       │ - AWS EC2       │
                       │ - Docker        │
                       │ - Jenkins CI/CD │
                       │ - Nginx         │
                       └─────────────────┘
```

## 🛠 기술 스택

### Backend
- **Spring Boot 3.x** - 메인 프레임워크
- **Spring Security** - 인증/인가
- **Spring Data JPA** - ORM
- **Spring Cache** - 캐싱
- **Spring Boot Actuator** - 모니터링

### Database
- **MySQL 8.0** - 메인 데이터베이스
- **Redis** - 캐싱 및 세션 저장

### Infrastructure
- **Docker** - 컨테이너화
- **Jenkins** - CI/CD
- **AWS EC2** - 서버
- **Nginx** - 리버스 프록시

### API Documentation
- **Swagger/OpenAPI 3** - API 문서 자동화
- **SpringDoc** - 문서 생성

## 📊 데이터베이스 설계 (ERD)

### 핵심 엔티티
1. **User** - 사용자 (관리자/일반 사용자)
2. **Post** - 게시글
3. **Tag** - 태그
4. **Comment** - 댓글
5. **Category** - 카테고리
6. **Analytics** - 방문자 통계
7. **File** - 업로드 파일

### 관계 설계
- User 1:N Post (작성자)
- Post N:M Tag (다대다)
- Post 1:N Comment (댓글)
- Post N:1 Category (카테고리)
- User 1:N Comment (댓글 작성자)

## 🔧 핵심 백엔드 기능

### 1. 인증/인가 시스템
- JWT 기반 토큰 인증
- Role-based Access Control (RBAC)
- OAuth2 소셜 로그인 (선택사항)

### 2. 게시글 관리 API
- CRUD 작업
- 검색 및 필터링 (Full-text search)
- 태그 기반 분류
- 조회수 추적

### 3. 댓글 시스템
- 계층형 댓글 (대댓글)
- 댓글 신고/차단 기능
- 스팸 방지

### 4. 통계 및 분석
- 방문자 로그 수집
- 인기 게시글 분석
- 사용자 행동 패턴 분석
- 실시간 대시보드 API

### 5. 파일 관리
- 이미지 업로드 (S3 연동)
- 파일 압축 및 최적화
- CDN 연동

### 6. 성능 최적화
- Redis 캐싱
- 데이터베이스 인덱싱
- 페이지네이션
- Lazy Loading

## 📝 API 설계

### 인증 API
```
POST /api/auth/login          - 로그인
POST /api/auth/logout         - 로그아웃
POST /api/auth/refresh        - 토큰 갱신
GET  /api/auth/profile        - 프로필 조회
PUT  /api/auth/profile        - 프로필 수정
```

### 게시글 API
```
GET    /api/posts             - 게시글 목록
GET    /api/posts/{id}        - 게시글 상세
POST   /api/posts             - 게시글 작성
PUT    /api/posts/{id}        - 게시글 수정
DELETE /api/posts/{id}        - 게시글 삭제
GET    /api/posts/search      - 게시글 검색
GET    /api/posts/popular     - 인기 게시글
```

### 댓글 API
```
GET    /api/posts/{id}/comments     - 댓글 목록
POST   /api/posts/{id}/comments     - 댓글 작성
PUT    /api/comments/{id}           - 댓글 수정
DELETE /api/comments/{id}           - 댓글 삭제
```

### 통계 API
```
GET /api/analytics/visitors         - 방문자 통계
GET /api/analytics/posts            - 게시글 통계
GET /api/analytics/popular          - 인기 콘텐츠
GET /api/analytics/dashboard        - 대시보드 데이터
```

## 🚀 배포 및 운영

### CI/CD 파이프라인
1. **Git Push** → GitHub Webhook
2. **Jenkins Build** → 테스트 실행
3. **Docker Image Build** → 이미지 생성
4. **AWS EC2 Deploy** → 자동 배포
5. **Health Check** → 서비스 상태 확인

### 모니터링
- Spring Boot Actuator
- 로그 수집 및 분석
- 성능 메트릭 수집
- 알림 시스템

## 📈 포트폴리오 포인트

### 1. 아키텍처 설계 능력
- 확장 가능한 시스템 설계
- 마이크로서비스 고려사항
- 데이터베이스 최적화

### 2. 보안 역량
- JWT 인증 구현
- SQL Injection 방지
- XSS/CSRF 보안
- Rate Limiting

### 3. 성능 최적화
- 캐싱 전략
- 데이터베이스 쿼리 최적화
- 비동기 처리

### 4. DevOps 역량
- Docker 컨테이너화
- CI/CD 파이프라인 구축
- 인프라 자동화

### 5. API 설계
- RESTful API 설계
- Swagger 문서화
- 버전 관리

## 🎯 차별화 포인트

1. **실제 운영 서비스** - 단순 프로젝트가 아닌 실제 사용되는 블로그
2. **완전한 백엔드 구현** - 프론트엔드에 의존하지 않는 독립적인 API
3. **확장성 고려** - 향후 기능 추가를 고려한 설계
4. **운영 경험** - 실제 서비스 운영을 통한 문제 해결 경험
5. **문서화** - API 문서, 아키텍처 문서 등 체계적인 문서화
