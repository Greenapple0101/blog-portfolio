# Portfolio Backend API

포트폴리오 웹사이트를 위한 백엔드 API 서버입니다.

## 주요 기능

- **프로젝트 관리**: 포트폴리오 프로젝트 CRUD 작업
- **블로그 시스템**: 게시글 작성, 댓글 시스템
- **연락처 관리**: 이메일 연락처 폼 처리
- **인증 시스템**: JWT 기반 관리자 인증
- **파일 업로드**: 이미지 업로드 및 관리
- **보안**: Rate limiting, CORS, Helmet 보안 헤더

## 기술 스택

- **Node.js** + **Express.js**
- **MongoDB** + **Mongoose**
- **JWT** 인증
- **Multer** 파일 업로드
- **Nodemailer** 이메일 전송
- **Helmet** 보안
- **CORS** 크로스 오리진

## 설치 및 실행

### 1. 의존성 설치
```bash
npm install
```

### 2. 환경 변수 설정
```bash
cp env.example .env
```

`.env` 파일을 편집하여 다음 값들을 설정하세요:
- `MONGODB_URI`: MongoDB 연결 문자열
- `JWT_SECRET`: JWT 토큰 시크릿 키
- `EMAIL_*`: 이메일 설정 (Gmail 등)
- `PORT`: 서버 포트 (기본값: 5000)

### 3. 서버 실행

**개발 모드:**
```bash
npm run dev
```

**프로덕션 모드:**
```bash
npm start
```

## API 엔드포인트

### 인증 (Authentication)
- `POST /api/auth/register` - 관리자 회원가입
- `POST /api/auth/login` - 로그인
- `GET /api/auth/me` - 현재 사용자 정보
- `PUT /api/auth/profile` - 프로필 업데이트
- `PUT /api/auth/password` - 비밀번호 변경

### 프로젝트 (Projects)
- `GET /api/projects` - 프로젝트 목록 조회
- `GET /api/projects/:id` - 특정 프로젝트 조회
- `POST /api/projects` - 프로젝트 생성 (관리자)
- `PUT /api/projects/:id` - 프로젝트 수정 (관리자)
- `DELETE /api/projects/:id` - 프로젝트 삭제 (관리자)

### 게시글 (Posts)
- `GET /api/posts` - 게시글 목록 조회
- `GET /api/posts/slug/:slug` - 슬러그로 게시글 조회
- `POST /api/posts` - 게시글 생성 (관리자)
- `PUT /api/posts/:id` - 게시글 수정 (관리자)
- `DELETE /api/posts/:id` - 게시글 삭제 (관리자)
- `POST /api/posts/:id/comments` - 댓글 추가

### 연락처 (Contact)
- `POST /api/contact` - 연락처 폼 제출
- `GET /api/contact` - 연락처 목록 조회 (관리자)
- `GET /api/contact/:id` - 특정 연락처 조회 (관리자)
- `PUT /api/contact/:id/status` - 연락처 상태 업데이트 (관리자)
- `POST /api/contact/:id/reply` - 연락처에 답장 (관리자)

## 데이터베이스 모델

### User (사용자)
- 관리자 계정 정보
- 프로필 정보 (이름, 소개, 소셜 링크)

### Project (프로젝트)
- 프로젝트 제목, 설명, 이미지
- 기술 스택, 카테고리, 링크
- 피처드 여부, 순서

### Post (게시글)
- 제목, 내용, 슬러그
- 태그, 카테고리, 상태
- 조회수, 좋아요 수

### Comment (댓글)
- 댓글 내용, 작성자 정보
- 게시글 연결, 대댓글 지원
- 승인 상태 관리

### Contact (연락처)
- 연락처 폼 데이터
- 상태 관리 (새 메시지, 읽음, 답장 등)
- 관리자 노트

## 보안 기능

- **JWT 토큰** 기반 인증
- **Rate Limiting** (15분당 100회 요청 제한)
- **CORS** 설정
- **Helmet** 보안 헤더
- **비밀번호 해싱** (bcrypt)
- **파일 업로드** 검증

## 배포

### Heroku 배포
1. Heroku CLI 설치
2. Heroku 앱 생성
3. 환경 변수 설정
4. MongoDB Atlas 연결
5. Git 푸시

### Vercel 배포
1. Vercel CLI 설치
2. `vercel` 명령어로 배포
3. 환경 변수 설정

### Docker 배포
```bash
# Dockerfile 생성 후
docker build -t portfolio-backend .
docker run -p 5000:5000 portfolio-backend
```

## 개발 가이드

### 코드 구조
```
├── models/          # 데이터베이스 모델
├── routes/          # API 라우트
├── middleware/      # 미들웨어 (인증, 업로드)
├── uploads/         # 업로드된 파일
├── server.js        # 메인 서버 파일
└── package.json     # 의존성 및 스크립트
```

### 새로운 기능 추가
1. 모델 정의 (`models/`)
2. 라우트 생성 (`routes/`)
3. 미들웨어 추가 (필요시)
4. 서버에 라우트 등록

### 테스트
```bash
npm test
```

## 라이선스

MIT License
