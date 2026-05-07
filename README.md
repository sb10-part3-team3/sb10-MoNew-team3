# 👥 2026-05-sb10-part3-team3

## 📰 관심 뉴스를 모아서 제공하는 뉴스 커뮤니티 서비스 [ Monew ]

<table align="center">
    <tr align="center">
        <td colspan="5">
            <p style="font-size: x-large; font-weight: bold;">2026년 5월 Spring Backend10기 특종JAVA (Team3)</p>
        </td>
    </tr>
    <tr align="center">
        <td style="min-width: 150px;">
            <a href="https://github.com/Minbro-Kim">
                <img src="https://avatars.githubusercontent.com/u/144206885?v=4" width="200" alt="김민형_깃허브프로필">
                <br />
                <b>Minbro-Kim</b>
            </a>
        </td>
        <td style="min-width: 150px;">
            <a href="https://github.com/shong9124">
                <img src="https://avatars.githubusercontent.com/u/162269186?v=4" width="200" alt="김하은_깃허브프로필">
                <br />
                <b>shong9124</b>
            </a>
        </td>
        <td style="min-width: 150px;">
            <a href="https://github.com/Gusals911">
                <img src="https://avatars.githubusercontent.com/u/180506548?v=4" width="200" alt="김현민_깃허브프로필">
                <br />
                <b>Gusals911</b>
            </a>
        </td>
        <td style="min-width: 150px;">
            <a href="https://github.com/Amperisk9">
                <img src="https://avatars.githubusercontent.com/u/59495660?v=4" width="200" alt="조성진_깃허브프로필">
                <br />
                <b>Amperisk9</b>
            </a>
        </td>
        <td style="min-width: 150px;">
            <a href="https://github.com/JunSuHwang">
                <img src="https://avatars.githubusercontent.com/u/85167454?v=4" width="200" alt="황준수_깃허브프로필">
                <br />
                <b>JunSuHwang</b>
            </a>
        </td>
    </tr>
    <tr align="center">
        <td>
            <b>김민형</b>
        </td>
        <td>
            <b>김하은</b>
        </td>
        <td>
            <b>김현민</b>
        </td>
        <td>
            <b>조성진</b>
        </td>
        <td>
            <b>황준수</b>
        </td>
    </tr>
    <tr align="center">
        <td>
            <b>Team Leader</b>, Backend
        </td>
        <td>
            Backend
        </td>
        <td>
            Backend
        </td>
        <td>
            Backend
        </td>
        <td>
            Backend
        </td>
    </tr>
</table>

## 📑 목차

1. [📰 프로젝트 소개](#-프로젝트-소개)
2. [🛠️ 기술 스택 및 아키텍처](#️-기술-스택-및-아키텍처)
3. [📊 주요 성과](#-주요-성과)
4. [🔗 프로젝트 문서 및 링크](#-프로젝트-문서-및-링크)
5. [🐳 프로젝트 실행 방법](#-프로젝트-실행-방법)
6. [🧑‍💻 팀원별 구현 기능 상세](#-팀원별-구현-기능-상세)
7. [📁 파일 구조](#-파일-구조)
8. [🏠 구현 홈페이지](#-구현-홈페이지)
9. [📝 프로젝트 개인 개발 리포트](#-프로젝트-개인-개발-리포트)

## 📰 프로젝트 소개

- 관심 뉴스 구독 서비스의 Spring 백엔드 시스템 구축
- 프로젝트 기간: 2026.04.11 ~ 2026.05.08
- 주요 기능
    - 사용자 관리
        - 이메일(아이디)과 닉네임, 비밀번호로 회원가입을 할 수 있습니다.
        - 사용자 닉네임을 수정할 수 있습니다.
        - 사용자 계정을 논리 삭제한 뒤, 1일 뒤 자동으로 물리삭제를 진행합니다.
        - 이메일과 비밀번호를 이용하여 로그인을 하고 `MoNew-Request-User-ID` 헤더를 통해 로그인 사용자에 대한 화면을 표시합니다.
    - 관심사 관리
        - 관심사의 이름과 관련 키워드를 입력하여 관심사를 등록할 수 있습니다.
            - 유사한 관심사 이름의 등록을 방지할 수 있습니다.
        - 특정 관심사의 키워드를 수정할 수 있습니다.
        - 관심사를 삭제할 수 있습니다.
        - 관심사 목록을 `이름`과 `키워드`에 대한 검색어로 필터링하고, `이름` 또는 `구독자 수`로 정렬하여 조회할 수 있습니다.
        - 관심사를 구독 및 구독 취소할 수 있습니다.
    - 뉴스 기사 관리
        - 관심사와 키워드의 조합으로`네이버`와 `조선일보`의 뉴스 기사를 매 시간 수집할 수 있습니다.
        - 뉴스 기사의 출처 목록을 조회할 수 있습니다.
        - 특정 뉴스 기사를 조회하고 조회 수를 증가 시킬 수 있습니다.
        - 뉴스 기사 목록을 `제목`과 `요약`에 대한 검색어 필터링과`관심사`와 `출처`, `날짜` 조건 필터링을 적용하고, `날짜` 또는 `댓글 수`, `조회 수`로
          정렬하여 조회할 수 있습니다.
        - 특정 뉴스 기사를 삭제할 수 있습니다.
        - 삭제된 뉴스 기사를 S3 백업 저장소를 통해 `날짜 범위`로 복구할 수 있습니다.
    - 댓글 관리
        - 뉴스 기사에 댓글을 등록할 수 있습니다.
        - 자신의 댓글을 수정하고 삭제할 수 있습니다.
        - 특정 뉴스 기사에 대한 댓글 목록을 `날짜`와 `좋아요 수`로 정렬하여 조회할 수 있습니다.
        - 좋아요를 등록하고 취소할 수 있습니다.
    - 활동 내역 관리
        - 사용자 별로 사용자의 정보와 구독 중인 관심사, 최근 작성한 댓글, 좋아요를 누른 댓글, 최근 확인한 뉴스 기사를 조회할 수 있습니다.
    - 알림 관리
        - 구독 중인 관심사에 대한 뉴스기사가 등록되거나 자신의 댓글이 좋아요를 받으면 알림을 받을 수 있습니다.
        - 확인하지 않은 알림 목록을 최신순으로 조회할 수 있습니다.
        - 알림을 개별 확인 또는 전체 확인 할 수 있고, 일주일 후에 확인한 알림을 자동으로 삭제합니다.
- 주요 화면

---

## 🛠️ **기술 스택 및 아키텍처**

### 1️⃣ 기술 스택

- 이미지 추가

### 2️⃣ 배포 다이어그램

## 📊 주요 성과

### 1️⃣ Test Coverage

- 테스트 커버리지 85% 이상 달성
    - [![codecov](https://codecov.io/gh/sb10-part3-team3/sb10-MoNew-team3/branch/main/graph/badge.svg?token=HH8VH1APA9)](https://codecov.io/gh/sb10-part3-team3/sb10-MoNew-team3)

## 🔗 프로젝트 문서 및 링크

| Category       | Link                                                                                                                    | Description                 |
|:---------------|:------------------------------------------------------------------------------------------------------------------------|:----------------------------|
| **Workspace**  | [Notion Workspace](https://www.notion.so/JAVA-682254ef76b6827585f281bb5eff9f63?source=copy_link)                        | 기획 문서, 회의록 및 팀 컨벤션 관리       |
| **Management** | [GitHub Issues & Gantt](https://github.com/orgs/sb10-part3-team3/projects/3/views/2)                                    | 이슈 카드를 활용한 역할 및 일정 관리       |
| **요구사항 명세서**   | [요구사항 명세서](https://docs.google.com/spreadsheets/d/1wDdlF1cUaX2ReYMBSfZH6NytVy9-uuuVviT5A4RkpgY/edit?usp=sharing)/pdf 추가 | 구글 스프레드 시트를 활용한 요구사항 명세 작성  |
| **API Docs**   | [Swagger UI](http://52.79.234.93/swagger-ui/index.html)/json추가                                                          | RESTful API 명세서             |
| **Design**     | [Database ERD](https://dbdiagram.io/d/69b1119c77d079431b56b7c9)/png추가                                                   | dbDiagram을 통한 데이터베이스 구조 설계도 |
| **프로젝트 계획서**   | [프로젝트 계획서](https://www.notion.so/344254ef76b6802c9cceda44f858d8c7)/pdf 추가                                               | 프로젝트 계획서                    |
| **발표 자료**      | [발표 자료](https://canva.link/cydmnvhakz6u2or)/pdf 추가                                                                      | 발표 자료                       |

-----

## 🐳 프로젝트 실행 방법

### 0️⃣ 기본 세팅

- `main` 브랜치 Clone
- `Naver` 뉴스 수집을 원하는 경우 키 발급 필요(
  참고 [Naver API](https://developers.naver.com/docs/serviceapi/search/news/news.md))
- 뉴스 백업 및 복구 기능 구현을 원하는 경우 S3 버킷 및 관련 AccessKey 발급 필요

### 1️⃣ .env 파일작성

- 프로젝트 루트에 위치한 .env.example 파일을 참고하여 .env 파일 작성

### 2️⃣ Docker Compose 실행

- 프로젝트 루트에 위치한 docker-compose.yml 파일 실행
- Docker Compose 볼륨을 사용하여 PostgresSql과 MongoDB 데이터 유지
- .env 파일 환경변수 적용
- sql 스크립트 자동 실행

#### 🐳 컨테이너 제어 명령어

- 컨테이너 실행

  ```bash
  docker-compose up
  ```
- 컨테이너 중지

  ```bash
  docker-compose down
  ```
- 컨테이너 볼륨 삭제

  ```bash
  docker-compose down -v
  ```

### 3️⃣ APP 실행

- Spring Boot를 `prod` profile로 설정하여 실행(기본 `dev`)
  ```bash
  # prod 프로필로 실행
  ./gradlew bootRun --args='--spring.profiles.active=prod'
  ```

---

## 🧑‍💻 **팀원별 구현 기능 상세**

<details>
  <summary><b>🏃 김민형 (Team Leader)</b></summary>
  <div markdown="1">

## 알림 관리 API

- **알림 등록 구현**:
  - 
- **슬라이스 페이징 기반 정렬 조회**:
    - **커서 기반 페이지네이션**구현
    - 첫 번째 페이지 조회 시 불필요한`Count`쿼리가 발생하지 않도록 최적화하여 DB 부하 감소
- **DTO Projection 최적화**:

## 구현기능

- **자세한 사항**:
    - 설명

## 성능 최적화 및 인프라 개선

- 설명

<img width="1437" alt="Image" src="https://github.com/user-attachments/assets/259036d9-bf1a-4262-ae04-8fa36c58b231" />
  </div>
</details>

<details>
  <summary><b>🏃 김하은</b></summary>
  <div markdown="1">

## 알림 관리 API

- **알림 등록 구현**:
  - 
- **슬라이스 페이징 기반 정렬 조회**:
    - **커서 기반 페이지네이션**구현
    - 첫 번째 페이지 조회 시 불필요한`Count`쿼리가 발생하지 않도록 최적화하여 DB 부하 감소
- **DTO Projection 최적화**:

## 구현기능

- **자세한 사항**:
    - 설명

## 성능 최적화 및 인프라 개선

- 설명

<img width="1437" alt="Image" src="https://github.com/user-attachments/assets/259036d9-bf1a-4262-ae04-8fa36c58b231" />
  </div>
</details>

<details>
  <summary><b>🏃 김현민</b></summary>
  <div markdown="1">

## 알림 관리 API

- **알림 등록 구현**:
  - 
- **슬라이스 페이징 기반 정렬 조회**:
    - **커서 기반 페이지네이션**구현
    - 첫 번째 페이지 조회 시 불필요한`Count`쿼리가 발생하지 않도록 최적화하여 DB 부하 감소
- **DTO Projection 최적화**:

## 구현기능

- **자세한 사항**:
    - 설명

## 성능 최적화 및 인프라 개선

- 설명

<img width="1437" alt="Image" src="https://github.com/user-attachments/assets/259036d9-bf1a-4262-ae04-8fa36c58b231" />
  </div>
</details>


<details>
  <summary><b>🏃 조성진</b></summary>
  <div markdown="1">

## 알림 관리 API

- **알림 등록 구현**:
  - 
- **슬라이스 페이징 기반 정렬 조회**:
    - **커서 기반 페이지네이션**구현
    - 첫 번째 페이지 조회 시 불필요한`Count`쿼리가 발생하지 않도록 최적화하여 DB 부하 감소
- **DTO Projection 최적화**:

## 구현기능

- **자세한 사항**:
    - 설명

## 성능 최적화 및 인프라 개선

- 설명

<img width="1437" alt="Image" src="https://github.com/user-attachments/assets/259036d9-bf1a-4262-ae04-8fa36c58b231" />
  </div>
</details>


<details>
  <summary><b>🏃 황준수</b></summary>
  <div markdown="1">

## 알림 관리 API

- **알림 등록 구현**:
  - 
- **슬라이스 페이징 기반 정렬 조회**:
    - **커서 기반 페이지네이션**구현
    - 첫 번째 페이지 조회 시 불필요한`Count`쿼리가 발생하지 않도록 최적화하여 DB 부하 감소
- **DTO Projection 최적화**:

## 구현기능

- **자세한 사항**:
    - 설명

## 성능 최적화 및 인프라 개선

- 설명

<img width="1437" alt="Image" src="https://github.com/user-attachments/assets/259036d9-bf1a-4262-ae04-8fa36c58b231" />
  </div>
</details>

## 📁 **파일 구조**

<details>
  <summary>파일 구조 (클릭)</summary>
  <div markdown="1">

```text
src/
├── 최종 완료시 수정 예정
```

  </div>
</details>

## 🏠 **구현 홈페이지**

<a href="http://52.79.234.93" target="_blank">
  <img src="이미지 주소" width="600px" alt="Monew Image" />
</a>

> **이미지를 클릭**하면 서비스 페이지로 연결됩니다.


---

## 📝 **프로젝트 개인 개발 리포트**

- [김민형](개인개발 레포트 주소 추가)
- [김하은]()
- [김현민]()
- [조성진]()
- [황준수]()