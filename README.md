# 🌐 ValanSee Backend Server
*TAVE 15기 연합동아리 ValanSee 팀의 서버 레포지토리*

<p align="center">
  <img src="https://img.shields.io/badge/TAVE-15기-blue?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Team-ValanSee-purple?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Backend-SpringBoot-green?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Database-MySQL-orange?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Build-Gradle-yellow?style=for-the-badge"/>
</p>

---

## 📌 프로젝트 소개  
**ValanSee**는 TAVE 15기 연합동아리에서 진행한 프로젝트로,  
사용자에게 **편리한 서비스 경험과 안정적인 데이터 관리**를 제공하기 위해 개발되었습니다.  
본 레포지토리는 **Spring Boot 기반 백엔드 서버**로서 API 제공, 인증, 데이터베이스 관리, 배포를 담당합니다.

---

## ✨ 주요 기능  
- 🔑 **회원가입 & 로그인**: Spring Security + JWT 기반 인증  
- 📡 **REST API 서버**: 프론트엔드와의 데이터 통신 제공  
- 📊 **데이터 관리**: MySQL 기반 CRUD API  
- ⚙️ **CI/CD 파이프라인 구축**: GitHub Actions 및 Docker 배포  
- 🔔 **알림 및 확장 기능**: 사용자 맞춤 알림, 통계 분석  

---

## 🛠 기술 스택  
- **Language**: Java 17  
- **Framework**: Spring Boot, Spring Security, Spring Data JPA  
- **Database**: MySQL  
- **Build Tool**: Gradle  
- **Deployment**: AWS EC2, Docker  
- **CI/CD**: GitHub Actions  
- **Collaboration Tools**: Notion, Figma, ERDCloud  

---

## ⚙️ 실행 방법  

```bash
# 1. 레포지토리 클론
git clone https://github.com/TAVE-ValanSee/Server.git
cd Server

# 2. 빌드 (Gradle 사용 시)
./gradlew build

# 3. 서버 실행
java -jar build/libs/valansee-server-0.0.1-SNAPSHOT.jar
```

## 🔐 민감 설정 관리

- `src/main/resources/application.yml`은 git에 커밋하지 않고 팀 내부 저장소에서만 관리합니다.
- 설정값 공유는 팀원 접근 권한이 제한된 공간에서만 진행하고, 외부 공유와 스크린샷 공유를 금지합니다.
- 팀원 변경이나 권한 회수가 필요할 때는 저장소 접근 권한을 함께 정리합니다.
- 운영 설정 변경 이력은 팀 문서에 남기고, 장기적으로는 환경변수 또는 secret manager 기반 주입으로 전환합니다.

## 🤖 콘텐츠 자동 생성(봇 시드) 운영 가이드

- 기본값은 모든 환경에서 비활성(`content-seed.enabled: false`)입니다. 코드 기본값이 `false`이고, `application.yml`의 dev/dev-server/prod 프로필 어디에도 이를 `true`로 재정의하는 설정이 없으므로 별도 조치 없이는 어떤 환경에서도 자동 실행되지 않습니다.
- prod에서 활성화하려면 운영자가 수동 검수 후 (git에 커밋되지 않는) prod 프로필 `application.yml`에 `content-seed.enabled: true`를 명시적으로 추가해야 합니다. dev·dev-server는 계속 비활성 상태로 둡니다.
- 필요한 환경변수: `ANTHROPIC_API_KEY`(Claude API 키). 값이 없으면 관리자 수동 실행 API(`POST /admin/content-seed/run`)는 즉시 503을 반환하고, 자동 스케줄은 생성 시점에 API 호출이 실패해 해당 봇/배치만 실패로 기록됩니다.
- Discord 실행 결과 알림은 서버 오류 알림과 동일한 `alert.discord.*` 설정(웹훅)을 재사용합니다. 별도의 웹훅을 새로 설정할 필요는 없으며, `alert.discord.enabled`가 꺼져 있거나 웹훅 전송이 실패해도 콘텐츠 생성 자체에는 영향이 없습니다.
- API Key와 웹훅 URL은 다른 민감 설정과 마찬가지로 저장소에 기록하지 않고 환경변수로만 주입합니다.

## 운영 DB 중복 데이터 점검 쿼리

현재 프로젝트에는 별도 migration 도구가 없으므로 중복 방지 제약은 우선 JPA `@Table(uniqueConstraints = ...)`로 반영되어 있습니다. 운영 DB에 unique 제약을 직접 추가하기 전에는 아래 쿼리로 기존 중복 데이터를 먼저 점검하고 정리해야 합니다.

```sql
-- 같은 사용자가 같은 투표에 여러 번 투표한 기록
select member_id, vote_id, count(*) as duplicate_count
from member_vote_option
group by member_id, vote_id
having count(*) > 1;

-- 같은 사용자가 같은 댓글에 여러 번 좋아요한 기록
select user_id, comment_id, count(*) as duplicate_count
from comment_like
group by user_id, comment_id
having count(*) > 1;

-- 같은 프로필이 같은 칭호를 여러 번 보유한 기록
select member_profile_id, title_id, count(*) as duplicate_count
from member_profile_title
group by member_profile_id, title_id
having count(*) > 1;
```
