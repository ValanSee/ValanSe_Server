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
