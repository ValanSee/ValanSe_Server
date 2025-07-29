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
