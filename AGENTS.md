# Global Tech Blog Archive

Engineering team blog archive service. Built for developers who want to discover technical articles, engineering stories, architecture decisions, and development experiences shared by engineering teams. Currently in MVP development.

## Tech Layers

- **Backend**: Java 21, Spring Boot 3.5, Spring Data JPA
- **Frontend**: React 19, TypeScript 5, Vite
- **Database**: MySQL 8.4 LTS

## Project Structure

```text
backend/      # Java 21, Spring Boot, Spring Data JPA
frontend/     # React, TypeScript, Vite
docs/         # Detailed design notes and decisions
```

## Key Files

```text
backend/
├── src/main/java/       # Backend application source code
├── src/test/java/       # Backend tests
└── build.gradle         # Backend build and dependency configuration

frontend/
└── TBD                  # Frontend application source code after Vite setup

docs/
└── git-workflow.md      # Branch, issue, commit, and PR workflow rules
```

## Commands

```bash
# Backend
cd backend
./gradlew test        # 단위 테스트
./gradlew build       # 빌드
./gradlew bootRun     # 개발 서버 (포트: 8080)

# Frontend
cd frontend
npm install           # 의존성 설치
npm run dev           # 개발 서버 (포트: 5173)
npm run build         # 타입체크 + 프로덕션 빌드
npm run lint          # 린터

# Database
docker compose up -d mysql    # MySQL 개발 DB 실행 (포트: 3306)
docker compose down           # MySQL 개발 DB 중지
```

- Backend typecheck: included in `./gradlew build`
- Frontend typecheck: included in `npm run build`
- E2E test: TBD
- 작업 완료 전 필수 체크: `cd backend && ./gradlew test build`, `cd frontend && npm run build && npm run lint`

## Engineering Principles

### Development Approach

* Prefer MVP over completeness.
* Prefer simple solutions over complex abstractions.
* Build functionality incrementally in small steps.
* Authentication is not required for the initial MVP.

### Change Management

Discuss the following changes before implementation:

* Architecture changes
* Database schema changes
* New dependency additions

### Documentation

- Detailed design decisions belong in docs/.
- Detailed Git workflow rules belong in docs/git-workflow.md.


## Common Mistakes to Avoid

TDB

## Git Workflow

Follow docs/git-workflow.md.
