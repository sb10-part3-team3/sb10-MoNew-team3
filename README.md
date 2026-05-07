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
6. [💻 팀원별 구현 기능 상세](#-팀원별-구현-기능-상세)
7. [📁 파일 구조](#-파일-구조)
8. [🏠 구현 홈페이지](#-구현-홈페이지)
9. [📝 프로젝트 개인 개발 리포트](#-프로젝트-개인-개발-리포트)

## 📰 프로젝트 소개

- 관심 뉴스 구독 서비스의 Spring 백엔드 시스템 구축
- 프로젝트 기간: 2026.04.14 ~ 2026.05.08
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
<img width="700" alt="image" src="https://github.com/user-attachments/assets/27d2f321-ec6c-446a-bd19-e0fe9bad9284" />



### 2️⃣ 배포 다이어그램
<img width="700" alt="image" src="https://github.com/user-attachments/assets/7be06350-c128-4302-87c9-8d7f2699fdcc" />

----

## 📊 주요 성과

### 1️⃣ Test Coverage

- 테스트 커버리지 85% 이상 달성
  [![codecov](https://codecov.io/gh/sb10-part3-team3/sb10-MoNew-team3/branch/main/graph/badge.svg?token=HH8VH1APA9)](https://codecov.io/gh/sb10-part3-team3/sb10-MoNew-team3)
      
  <img width="700" alt="image" src="https://github.com/user-attachments/assets/408dc780-bd88-4c5a-80e1-f47aee12df67" />

### 2️⃣ 배치 작업 모니터링
- Spring Batch 작업별로 실행 횟수, 성공·실패 여부, 처리 건수, 실행 시간 등의 커스텀 메트릭 지표 수집 (Spring Actuator)
  
  <img width="700" alt="image" src="https://github.com/user-attachments/assets/7d62e14a-fc40-4365-b1d8-aa8d4bb131a1" />

### 3️⃣ 부하테스트

<details>
  <summary>사용자 API</summary>
    <img width="700" alt="사용자 부하테스트" src="https://github.com/user-attachments/assets/6fe1e5ba-a00d-4602-b329-afaa4f3a9dc6" />
</details>
<details>
  <summary>알림 API</summary>
    <img width="700"alt="관심사 부하테스트" src="https://github.com/user-attachments/assets/80390084-4ede-4372-a317-00fe526413c0" />
</details>
<details>
  <summary>알림 API</summary>
   <img width="700" alt="알림 부하테스트" src="https://github.com/user-attachments/assets/3ea5398a-149a-44ca-af07-0af81d980829" />
</details>
  




----

## 🔗 프로젝트 문서 및 링크

| Category       | Link                                                                                                                    | Description                 |
|:---------------|:------------------------------------------------------------------------------------------------------------------------|:----------------------------|
| **Workspace**  | [Notion Workspace](https://www.notion.so/JAVA-682254ef76b6827585f281bb5eff9f63?source=copy_link)                        | 기획 문서, 회의록 및 팀 컨벤션 관리       |
| **Management** | [GitHub Issues & Gantt](https://github.com/orgs/sb10-part3-team3/projects/3/views/2)                                    | 이슈 카드를 활용한 역할 및 일정 관리       |
| **요구사항 명세서**   | [요구사항 명세서](https://docs.google.com/spreadsheets/d/1wDdlF1cUaX2ReYMBSfZH6NytVy9-uuuVviT5A4RkpgY/edit?usp=sharing)/[요구사항명세서.pdf](https://github.com/user-attachments/files/27497005/SB10-MoNew-Team3-.-.-3.pdf) | 구글 스프레드 시트를 활용한 요구사항 명세 작성  |
| **API Docs**   | [Swagger UI](http://52.79.234.93/swagger-ui/index.html)/[monew-api.json](https://github.com/user-attachments/files/27497078/monew-api.json)                                                          | RESTful API 명세서             |
| **Design**     | [Database ERD](https://dbdiagram.io/d/69b1119c77d079431b56b7c9)/[ERD.png](https://github.com/user-attachments/assets/b619dcf8-4890-4d53-bbdc-9bd53e322f38)| dbDiagram을 통한 데이터베이스 구조 설계도 |
| **프로젝트 계획서**   | [프로젝트 계획서](https://www.notion.so/344254ef76b6802c9cceda44f858d8c7)/[프로젝트_계획서.pdf](https://github.com/user-attachments/files/27497172/_.pdf)                                               | 프로젝트 계획서                    |
| **발표 자료**      | [발표 자료](https://canva.link/cydmnvhakz6u2or)/[Monew_NEWSJAVA_Team3.pdf](https://github.com/user-attachments/files/27497305/Monew_NEWSJAVA_Team3.pdf)| 발표 자료                       |
| **시연 영상**      | [시연 영상](https://drive.google.com/file/d/1DWs7WOC0UZxpHCz75v-MjSG-NSntkSGx/view?usp=share_link)| 시연 영상                      |

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

## 💻 **팀원별 구현 기능 상세**

<details>
  <summary><b>🏃 김민형 (Team Leader)</b></summary>
  <div markdown="1">

## 알림 관리 API

- **이벤트 기반 알림 아키텍처 설계**
  - 서비스 간 결합도 해소를 위해 Spring Event 기반 알림 생성 및 저장 로직 구현
  - `TransactionPhase.AFTER_COMMIT` 설정을 통해 메인 비즈니스 로직 성공 시에만 알림 발행 보장
  - 알림 유형(좋아요, 관심사 뉴스)별 스레드 풀 분리를 통해 시스템 자원 격리 및 안정성 확보
  - `getReferenceById` 프록시 객체 활용으로 불필요한 엔티티 조회 쿼리 방지

- **조회 및 업데이트 최적화**
  - 커서(ID)와 보조 커서(시간)를 결합한 복합 커서 방식의 페이지네이션 적용
  - 복합 인덱스(`user_id`, `is_confirmed`, `created_at DESC`) 생성으로 조회 성능 개선
  - 첫 페이지 조회 시 불필요한 `Count` 쿼리가 발생하지 않도록 슬라이스 처리 최적화
  - 벌크 업데이트 쿼리를 적용하여 대량 알림 확인 처리 시의 DB I/O 부하 감소

- **데이터 생명주기 관리 및 배치 처리**
  - Spring Batch Tasklet을 활용한 미확인/오래된 알림 자동 삭제 로직 구축
  - Native Query와 `LIMIT` 절을 조합하여 배치 사이즈 단위로 분할 삭제함으로써 DB 락(Lock) 경합 최소화
  - 새벽 시간대(03:00 KST) 스케줄링을 통한 안정적인 데이터 클리닝

## 인프라 및 배포 자동화 (CI/CD)

- **컨테이너 빌드 최적화**
  - Docker Multi-stage Build를 적용하여 최종 배포 이미지 크기 최소화 및 빌드 속도 개선
- **AWS 인프라 구축 및 운영**
  - ECR, S3, ECS(EC2), RDS 환경 구축 및 연동
  - ECS Task Definition 자동화(JSON)를 통한 신규 이미지 배포 파이프라인 구축
- **서버리스 로그 백업**
  - CloudWatch 로그를 AWS Lambda 및 이벤트 스케줄러를 활용하여 매일 S3로 자동 백업

## 개발 환경 및 테스트 전략

- **프로파일 기반 환경 관리**
  - test, dev, prod, data-gen 등 환경별 설정 분리 및 관리
  - 로컬 실행을 위한 Docker Compose 구성(DB 볼륨 처리 및 초기 스크립트 자동 실행)
- **테스트 신뢰성 확보**
  - Testcontainers를 싱글톤 패턴으로 구성하여 통합 테스트 환경의 일관성확보
  - Codecov 연동을 통해 CI 단계에서 테스트 커버리지 및 코드 품질 모니터링
- **성능 검증 환경 구축**
  - Locust를 활용한 부하 테스트 환경 설정 및 테스트 시나리오(locustfile.py) 작성
  - Instancio 라이브러리와 JDBC Batch Insert를 활용한 대량 더미 데이터 생성기 구축

  </div>
</details>

<details>
  <summary><b>🏃 김하은</b></summary>
  <div markdown="1">

## 관심사 관리 API
- **슬라이스 페이징 기반 정렬 조회**:
    - **커서 기반 페이지네이션** 구현
    - `name`, `subscriberCount` 기준 정렬 및 다중 정렬 조건 처리
    - 정렬 안정성을 위해 `createdAt` 기반 보조 정렬 적용
    - 첫 번째 페이지 조회 시 불필요한 `Count` 쿼리가 발생하지 않도록 최적화하여 DB 부하 감소
    - `limit + 1` 방식으로 다음 페이지 존재 여부(`hasNext`) 판별

- **DTO Projection 최적화**:
    - 관심사 목록 조회 시 DTO Projection을 활용하여 필요한 데이터만 조회
    - 구독 여부(`subscribedByMe`) 조회 시 N+1 문제를 방지하기 위해 사용자 구독 정보를 일괄 조회하도록 개선
    - `@BatchSize(size = 100)` 적용으로 Lazy Collection 조회 성능 개선

- **관심사 검색 기능 구현**:
    - 관심사 이름 및 키워드 기반 검색 기능 구현
    - QueryDSL 기반 동적 검색 조건 처리
    - `exists` 서브쿼리를 활용하여 중복 데이터 없이 관심사 검색 수행

- **관심사 유사도 검사 구현**:
    - 중복 관심사 등록 방지를 위한 문자열 유사도 검사 기능 구현
    - 한글 초성/중성/종성 분해 기반 비교 로직 적용
    - 특정 길이 이상의 문자열에 대해서만 유사도 검사를 수행하도록 최적화
    - 완전 동일 문자열과 유사 문자열을 구분하여 예외 처리

- **구독 처리 및 데이터 정합성 개선**:
    - 관심사 구독/구독 취소 시 `subscriberCount` 동기화 처리
    - JPQL Update Query를 활용하여 구독자 수를 원자적으로 증가/감소 처리
    - `subscriberCount > 0` 조건을 추가하여 음수 감소 방지
    - DB Unique Constraint 기반 중복 구독 방지 처리

- **예외 처리 및 검증 강화**:
    - 중복 관심사 등록, 삭제된 관심사 조회, 중복 구독 요청 등에 대한 커스텀 예외 처리 구현
    - 잘못된 커서 요청에 대한 예외 처리 및 안정성 강화
    - 글로벌 예외 처리기를 통한 일관된 에러 응답 형식 제공

## 구현기능

- **자세한 사항**:
    - 관심사 등록/수정/삭제 및 구독 기능 제공
    - 관심사 기반 뉴스 추천 및 사용자 맞춤 관심사 관리 지원
    - 대용량 데이터 조회 환경을 고려한 성능 최적화 적용
    - QueryDSL, DTO Projection, Cursor Pagination 등을 활용하여 조회 성능 개선
    - 테스트 코드(Unit Test / Integration Test / Controller Slice Test) 기반 안정성 확보
  </div>
</details>

<details>
  <summary><b>🏃 김현민</b></summary>
  <div markdown="1">

## 전역 로깅 및 운영 관찰성 개선
- **전역 로깅 처리 및 구조 정리**:
    - Logback, MDC, AOP 기반 전역 로깅 처리 적용
    - 요청 단위 로그 추적이 가능하도록 공통 로깅 컨텍스트 구성
    - 반복적인 개별 로깅 코드를 줄이고 일관된 로그 포맷 유지

- **배치 모니터링 도입**:
    - Spring Actuator + CloudWatch 기반 배치 모니터링 기능 구현
    - 뉴스 수집, 알림 삭제, 사용자 삭제, 기사 백업 배치에 대한 커스텀 메트릭 기록
    - 실행 횟수, 성공/실패 수, 실행 시간, 처리 건수 등을 메트릭으로 수집
    - 운영 환경에서 배치 상태를 외부 지표로 확인할 수 있도록 구성

## 댓글 관리 API
- **댓글 CRUD 전반 구현**:
    - 기사 기준 댓글 등록/수정/삭제/조회 기능 구현
    - 댓글 작성자 권한 검증 및 예외 처리 적용
    - 댓글 삭제 시 논리 삭제와 물리 삭제를 구분하여 처리
    - 댓글 삭제와 함께 기사 `commentCount` 동기화 처리

- **커서 기반 댓글 조회 구현**:
    - `createdAt`, `likeCount` 기준 정렬 조회 지원
    - `createdAt + id`, `likeCount + createdAt + id` 기반 복합 커서 처리
    - `limit + 1` 방식으로 다음 페이지 존재 여부(`hasNext`) 판별
    - 정렬 안정성과 대용량 조회 환경을 고려한 댓글 페이징 구조 적용

- **댓글 좋아요 기능 구현**:
    - 댓글 좋아요 등록/취소 기능 구현
    - `(comment_id, user_id)` Unique Constraint 기반 중복 좋아요 방지
    - 좋아요 수 증가/감소 시 댓글 `likeCount` 동기화 처리
    - 요청 사용자 기준 좋아요 여부 확인을 위한 일괄 조회 방식 적용

- **리팩터링 및 정합성 개선**:
    - 댓글 생성/수정/삭제/좋아요 흐름을 서비스 기준으로 정리
    - 파라미터 파싱, 커서 처리, 응답 변환 로직 리팩터링
    - 활동내역/알림과 연결되는 댓글 이벤트 흐름까지 고려하여 구조 개선

## 기사 관련 기능
- **기사 조회 이력 기능 구현**:
    - 기사 조회 시 사용자별 조회 이력 등록 기능 구현
    - 기사 조회수 증가와 사용자 활동 기록이 함께 반영되도록 구성
    - 기사 조회 기능을 별도 파일로 분리하되 기사 하위 기능으로 동작하도록 설계

- **기사 출처 목록 조회 기능 구현 및 구조 정리**:
    - 기사 출처 목록 조회 API 구현
    - 초기 분리 구현 이후 메인 기사 API/서비스 구조로 병합
    - 기사 도메인 안에서 출처목록 기능이 자연스럽게 관리되도록 리팩터링
    - 테스트 또한 메인 기사 테스트 구조 안으로 통합하여 일관성 확보

## 테스트 데이터 생성기 구현
- **뉴스기사/댓글 더미 데이터 생성기 구현**:
    - 부하 테스트 및 시연을 위한 뉴스기사 더미 생성기 구현
    - 댓글 더미 생성기 구현
    - `data-gen` 프로필 기반 실행 구조 적용
    - `JdbcTemplate.batchUpdate` 기반 대량 데이터 insert 처리

- **도메인 의존성 반영 및 정합성 고려**:
    - 뉴스기사 생성은 `NewsSource`를 부모 데이터로 사용
    - 댓글 생성은 `User + NewsArticle` 더미 데이터에 의존하도록 구성
    - 외래키 관계를 유지한 실제 서비스형 더미 데이터 생성 구조 적용
    - 댓글 생성 후 기사 `commentCount` 정합성도 함께 고려

## 댓글 부하 테스트 스크립트 작성
- **Locust 기반 댓글 부하 테스트 구현**:
    - 댓글 조회 및 댓글 등록 시나리오 중심의 부하 테스트 스크립트 작성
    - 기존 performance-test 구조를 유지하면서 `CommentTasks` 추가
    - 사용자/기사 ID pool 기반 실제 API 호출 시나리오 구성
    - 약한 부하부터 강한 부하까지 단계별 검증이 가능하도록 설계

- **테스트 실행 기반 마련**:
    - 테스트용 데이터 준비 흐름과 Locust 실행 구조 정리
    - 강한 부하 상황에서 댓글 등록/조회 API의 병목 및 500 에러 구간 확인
    - 배포 후에도 재사용 가능한 댓글 성능 테스트 기반 마련

## 구현기능

- **자세한 사항**:
    - 전역 로깅, 배치 모니터링, 댓글 도메인 전반, 기사 조회 이력/출처 목록, 테스트 데이터 생성기, 댓글 부하 테스트까지 구현
    - 사용자 기능뿐 아니라 운영 모니터링과 성능 검증 기반까지 함께 구축
    - Cursor Pagination, 배치 메트릭, 대량 데이터 생성, 부하 테스트 등을 활용하여 기능/운영/검증 전반을 개선
    - 테스트 코드(Unit Test / Controller Slice Test / Integration Test) 기반 안정성 확보
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

## 공통 예외 구조

- **`BusinessException` 기반 예외 계층 설계**:
    - 모든 도메인 예외가 `BusinessException`을 상속하는 단일 예외 계층 구조 구축
    - `ErrorCode` enum에 HTTP 상태 코드와 메시지를 함께 관리하여 예외 정보를 일관되게 유지
    - 예외 생성 시 `details` 필드를 통해 컨텍스트 정보(userId, field명 등)를 구조화된 형태로 전달

- **`GlobalExceptionHandler` 구현**:
    - `@RestControllerAdvice` 기반 전역 예외 처리기 구현으로 컨트롤러 레이어에서 예외 처리 코드 제거
    - `BusinessException`, `MethodArgumentNotValidException`, `MethodArgumentTypeMismatchException`, `MissingRequestHeaderException` 등 주요 예외 유형별 핸들러 분리
    - 5xx 서버 오류는 `log.error`, 4xx 클라이언트 오류는 `log.warn`으로 로그 레벨을 구분하여 운영 노이즈 감소

- **`ErrorResponse` 구조 설계**:
    - `timestamp`, `code`, `message`, `details`, `status` 필드를 포함한 일관된 에러 응답 포맷 구현
    - Java `record` 타입으로 설계하여 불변성 및 간결성 확보
    - `BusinessException`, `MethodArgumentNotValidException`, `MethodArgumentTypeMismatchException`, 일반 `Exception` 각각에 대한 정적 팩토리 메서드(`of`) 제공

- **민감 정보 로그 차단 처리**:
    - `password`, `token`, `apiKey`, `credential` 등 민감 키 목록을 정의하고, 로그 출력 전 해당 필드를 `[REDACTED]`로 마스킹
    - 키 정규화(대소문자·특수문자 제거) 및 중첩 `Map` / `List`에 대한 재귀 처리로 중첩 구조 내 민감 정보 누락 방지
    - 긴 문자열 값은 200자로 잘라 로그 과부하 방지, 바이너리 데이터는 `[BINARY]`로 치환

---

## 사용자 관리 API

- **회원가입 구현**:
    - `PasswordEncoder`를 통한 비밀번호 암호화 후 저장
    - 애플리케이션 레벨 중복 이메일 검증과 DB Unique Constraint 기반 이중 방어로 동시성 상황의 중복 가입 방지
    - 회원가입 완료 시 `UserRegisteredEvent` 이벤트 발행으로 MongoDB 활동 내역 문서 생성 연동

- **로그인 구현**:
    - 이메일·비밀번호 불일치 및 소프트 삭제 계정 여부를 동일한 `AuthException`으로 처리하여 정보 노출 방지

- **사용자 수정 구현**:
    - 닉네임 변경 시 `UserUpdatedEvent` 발행으로 MongoDB 활동 내역 문서(댓글·좋아요 내 닉네임)까지 일괄 동기화

- **논리 삭제 / 물리 삭제 분리**:
    - 소프트 삭제(`DELETE /api/users/{userId}`): `deletedAt` 타임스탬프 기록 방식의 논리 삭제
    - 하드 삭제(`DELETE /api/users/{userId}/hard`): 알림·댓글 좋아요·댓글·구독·기사 조회 이력 등 연관 데이터 순차 삭제 후 사용자 물리 삭제
    - `UserDeletedEvent` 발행으로 MongoDB 활동 내역 문서 삭제 연동

---

## 사용자 배치 처리

- **Spring Batch 기반 사용자 물리 삭제 배치 구현**:
    - `@Scheduled` + `JobLauncher` 조합으로 매일 새벽 2시 3분(기본값) 자동 실행
    - `runTime` JobParameter를 통해 실행 시각 기준으로 대상 사용자를 결정하여 재실행 시에도 동일한 기준 유지

- **`UserDeleteTasklet` 구현**:
    - 소프트 삭제 후 `retentionDays`(기본 1일) 경과한 사용자를 `batchSize`(기본 100건) 단위로 반복 처리
    - `RepeatStatus.CONTINUABLE` 반환으로 대상이 없을 때까지 루프 처리하여 대량 데이터 안전 처리
    - 알림 → 댓글 좋아요 → 댓글 → 구독 → 기사 조회 이력 → 활동 내역 순서로 외래키 제약 고려한 연관 데이터 삭제
    - ExecutionContext에 `userDelete.deletedCount`를 누적 기록하여 배치 완료 후 처리 건수 추적

- **배치 모니터링 연동**:
    - `BatchMetrics`를 통해 배치 성공/실패 여부, 실행 시간, 처리 건수를 CloudWatch 메트릭으로 수집
    - `batchSize`, `retentionDays`, cron 표현식을 외부 설정(`application.yml`)으로 분리하여 환경별 유연한 조정 가능
    - `@PostConstruct` 시점에 설정값 유효성 검증으로 잘못된 설정으로 인한 런타임 오류 사전 차단

---

## 사용자 활동 내역

- **MongoDB 문서 기반 활동 내역 관리**:
    - 사용자별 구독 중인 관심사, 최근 작성 댓글, 좋아요 한 댓글, 최근 확인한 기사를 단일 MongoDB 문서(`UserActivityDocument`)에서 통합 관리
    - 이벤트 기반 아키텍처로 사용자 등록·수정·삭제 등의 변경 사항을 비동기적으로 활동 내역에 반영

- **낙관적 락 기반 동시성 처리**:
    - 활동 내역 업데이트 시 `OptimisticLockingFailureException` 발생 시 `@Retryable`(최대 3회, 100ms 백오프)로 자동 재시도
    - 재시도 소진 시 `@Recover`를 통해 `UserActivityConflictException`으로 전환하여 클라이언트에 명시적 오류 전달
    - `getOrCreate` 패턴을 통해 문서 부재 시 빈 문서를 생성하여 동시 이벤트로 인한 누락 방지

- **닉네임 변경 시 전파 처리**:
    - 닉네임 수정 시 해당 사용자의 활동 내역 문서뿐 아니라 다른 사용자의 활동 내역 내 `comments`·`commentLikes` 임베디드 필드까지 일괄 업데이트하여 데이터 정합성 유지

- **삭제 시 연관 데이터 정리**:
    - 사용자 삭제 시 다른 사용자의 활동 내역 문서에 포함된 해당 사용자 댓글 기반의 좋아요 요약 제거
    - 기사 삭제 시 전체 사용자 문서에서 기사 조회 이력·댓글 요약·댓글 좋아요 요약을 일괄 제거
    - `TransientDataAccessException`에 대해서도 재시도 처리를 적용하여 일시적 MongoDB 오류 대응

## 구현기능

- **자세한 사항**:
    - 공통 예외 구조, 사용자 관리 API, 사용자 배치 처리, 사용자 활동 내역 전반 구현
    - 민감 정보 마스킹, 낙관적 락 재시도, 배치 모니터링 등 운영 안정성을 고려한 설계 적용
    - Spring Batch, Spring Retry, MongoDB 이벤트 기반 연동, 이중 중복 방지 처리 등을 활용하여 기능/운영/정합성 전반을 개선
    - 테스트 코드(Unit Test / Controller Slice Test / Integration Test) 기반 안정성 확보

  </div>
</details>

## 📁 **파일 구조**

<details>
  <summary>파일 구조 (클릭)</summary>
  <div markdown="1">

```text
src/
├── main/
│   ├── java/
│   │   ├── com/
│   │       ├── team3/
│   │           ├── monew/
│   │               ├── batch/
│   │               │   ├── job/
│   │               │   │   ├── ArticleBackupBatchConfig.java
│   │               │   │   ├── ArticleCollectBatchConfig.java
│   │               │   │   ├── NotificationDeleteBatchConfig.java
│   │               │   │   └── UserDeleteBatchConfig.java
│   │               │   ├── scheduler/
│   │               │   │   ├── ArticleBackupBatchScheduleConfig.java
│   │               │   │   ├── ArticleCollectBatchScheduleConfig.java
│   │               │   │   ├── NotificationDeleteScheduleConfig.java
│   │               │   │   └── UserDeleteScheduler.java
│   │               │   ├── tasklet/
│   │               │       └── UserDeleteTasklet.java
│   │               ├── component/
│   │               │   ├── news/
│   │               │       ├── client/
│   │               │       │   ├── ChosunNewsClient.java
│   │               │       │   ├── NaverNewsClient.java
│   │               │       │   └── NewsClient.java
│   │               │       ├── collect/
│   │               │       │   ├── ChosunNewsCollect.java
│   │               │       │   ├── NaverNewsCollect.java
│   │               │       │   ├── NewsCollect.java
│   │               │       │   └── NewsCollector.java
│   │               │       ├── filter/
│   │               │       │   ├── KeywordMatch.java
│   │               │       │   └── NewsFilter.java
│   │               │       ├── parse/
│   │               │       │   ├── ChosunNewsParse.java
│   │               │       │   ├── NaverNewsParse.java
│   │               │       │   ├── NewsParse.java
│   │               │       │   └── NewsParser.java
│   │               │       ├── record/
│   │               │           ├── ParsedData.java
│   │               │           ├── ParsedNewsArticle.java
│   │               │           └── RawArticleResult.java
│   │               ├── config/
│   │               │   ├── AsyncConfig.java
│   │               │   ├── AwsProperties.java
│   │               │   ├── BatchConfig.java
│   │               │   ├── CloudWatchConfig.java
│   │               │   ├── JacksonConfig.java
│   │               │   ├── JpaAuditingConfig.java
│   │               │   ├── MongoConfig.java
│   │               │   ├── NaverProperties.java
│   │               │   ├── PasswordEncoderConfig.java
│   │               │   ├── QueryDslConfig.java
│   │               │   ├── S3Config.java
│   │               │   ├── SchedulingConfig.java
│   │               │   ├── SwaggerConfig.java
│   │               │   └── WebConfig.java
│   │               ├── controller/
│   │               │   ├── api/
│   │               │   │   ├── ArticleApi.java
│   │               │   │   ├── ArticleViewApi.java
│   │               │   │   ├── CommentApi.java
│   │               │   │   ├── InterestApi.java
│   │               │   │   ├── NotificationApi.java
│   │               │   │   ├── UserActivityApi.java
│   │               │   │   └── UserApi.java
│   │               │   ├── ArticleController.java
│   │               │   ├── ArticleViewController.java
│   │               │   ├── CommentController.java
│   │               │   ├── InterestController.java
│   │               │   ├── NotificationController.java
│   │               │   ├── UserActivityController.java
│   │               │   └── UserController.java
│   │               ├── document/
│   │               │   ├── ArticleViewSummary.java
│   │               │   ├── CommentLikeSummary.java
│   │               │   ├── CommentSummary.java
│   │               │   ├── SubscriptionSummary.java
│   │               │   ├── UserActivityDocument.java
│   │               │   └── UserActivityRequest.java
│   │               ├── dto/
│   │               │   ├── article/
│   │               │   │   ├── internal/
│   │               │   │   │   ├── enums/
│   │               │   │   │   │   ├── ArticleDirection.java
│   │               │   │   │   │   └── ArticleOrderBy.java
│   │               │   │   │   ├── ArticleCursor.java
│   │               │   │   │   └── ArticleSearchCondition.java
│   │               │   │   ├── ArticleBackup.java
│   │               │   │   ├── ArticleDto.java
│   │               │   │   ├── ArticleRestoreResultDto.java
│   │               │   │   ├── ArticleSearchRequest.java
│   │               │   │   └── ArticleViewDto.java
│   │               │   ├── comment/
│   │               │   │   ├── CommentDto.java
│   │               │   │   ├── CommentLikeDto.java
│   │               │   │   ├── CommentRegisterRequest.java
│   │               │   │   ├── CommentUpdateRequest.java
│   │               │   │   └── CursorPageResponseCommentDto.java
│   │               │   ├── interest/
│   │               │   │   ├── internal/
│   │               │   │   │   ├── InterestCursor.java
│   │               │   │   │   └── InterestSearchCondition.java
│   │               │   │   ├── InterestDto.java
│   │               │   │   ├── InterestRegisterRequest.java
│   │               │   │   ├── InterestUpdateRequest.java
│   │               │   │   └── SubscriptionDto.java
│   │               │   ├── notification/
│   │               │   │   ├── CommentLikedNotificationRequest.java
│   │               │   │   ├── InterestNotificationRequest.java
│   │               │   │   └── NotificationDto.java
│   │               │   ├── pagination/
│   │               │   │   └── CursorPageResponseDto.java
│   │               │   ├── user/
│   │               │   │   ├── UserDto.java
│   │               │   │   ├── UserLoginRequest.java
│   │               │   │   ├── UserRegisterRequest.java
│   │               │   │   └── UserUpdateRequest.java
│   │               │   ├── useractivity/
│   │               │       └── UserActivityDto.java
│   │               ├── entity/
│   │               │   ├── base/
│   │               │   │   ├── BaseEntity.java
│   │               │   │   └── SoftDeleteEntity.java
│   │               │   ├── enums/
│   │               │   │   ├── BackupJobStatus.java
│   │               │   │   ├── BackupJobType.java
│   │               │   │   ├── DeleteStatus.java
│   │               │   │   ├── NewsSourceType.java
│   │               │   │   └── NotificationResourceType.java
│   │               │   ├── ArticleBackupJob.java
│   │               │   ├── ArticleInterest.java
│   │               │   ├── ArticleView.java
│   │               │   ├── Comment.java
│   │               │   ├── CommentLike.java
│   │               │   ├── Interest.java
│   │               │   ├── InterestKeyword.java
│   │               │   ├── NewsArticle.java
│   │               │   ├── NewsSource.java
│   │               │   ├── Notification.java
│   │               │   ├── Subscription.java
│   │               │   └── User.java
│   │               ├── event/
│   │               │   ├── ArticleDeletedEvent.java
│   │               │   ├── ArticleViewEvent.java
│   │               │   ├── CommentDeletedEvent.java
│   │               │   ├── CommentLikedActivityEvent.java
│   │               │   ├── CommentLikedEvent.java
│   │               │   ├── CommentRegisteredEvent.java
│   │               │   ├── CommentUnlikedEvent.java
│   │               │   ├── CommentUpdatedEvent.java
│   │               │   ├── InterestDeletedEvent.java
│   │               │   ├── InterestKeywordUpdatedEvent.java
│   │               │   ├── InterestNotificationEvent.java
│   │               │   ├── SubscriptionCanceledEvent.java
│   │               │   ├── SubscriptionEvent.java
│   │               │   ├── UserDeletedEvent.java
│   │               │   ├── UserRegisteredEvent.java
│   │               │   └── UserUpdatedEvent.java
│   │               ├── exception/
│   │               │   ├── article/
│   │               │   │   ├── ArticleBackupJobNotFoundException.java
│   │               │   │   ├── ArticleException.java
│   │               │   │   ├── ArticleInvalidPeriodException.java
│   │               │   │   ├── ArticleNotFoundException.java
│   │               │   │   ├── ArticleRequestInvalidException.java
│   │               │   │   └── DeletedArticleException.java
│   │               │   ├── comment/
│   │               │   │   ├── CommentException.java
│   │               │   │   ├── CommentLikeAlreadyExistsException.java
│   │               │   │   ├── CommentLikeNotFoundException.java
│   │               │   │   ├── CommentNotFoundException.java
│   │               │   │   ├── DeletedCommentException.java
│   │               │   │   └── UnauthorizedCommentUpdateException.java
│   │               │   ├── interest/
│   │               │   │   ├── InterestDuplicateNameException.java
│   │               │   │   ├── InterestException.java
│   │               │   │   └── InterestNotFoundException.java
│   │               │   ├── news/
│   │               │   │   ├── NewsClientException.java
│   │               │   │   ├── NewsException.java
│   │               │   │   └── NewsIllegalBeanException.java
│   │               │   ├── notification/
│   │               │   │   ├── NotificationConfirmForbiddenException.java
│   │               │   │   ├── NotificationException.java
│   │               │   │   └── NotificationNotFoundException.java
│   │               │   ├── user/
│   │               │   │   ├── AuthException.java
│   │               │   │   ├── DeletedUserException.java
│   │               │   │   ├── DuplicateEmailException.java
│   │               │   │   ├── InvalidNicknameException.java
│   │               │   │   ├── InvalidPasswordException.java
│   │               │   │   ├── UserException.java
│   │               │   │   └── UserNotFoundException.java
│   │               │   ├── useractivity/
│   │               │       ├── UserActivityConflictException.java
│   │               │       ├── UserActivityException.java
│   │               │       └── UserActivityNotFoundException.java
│   │               ├── global/
│   │               │   ├── enums/
│   │               │   │   └── ErrorCode.java
│   │               │   ├── exception/
│   │               │   │   ├── BusinessException.java
│   │               │   │   └── GlobalExceptionHandler.java
│   │               │   ├── init/
│   │               │   │   └── NewsSourceInitializer.java
│   │               │   ├── logging/
│   │               │   │   ├── ControllerLoggingAspect.java
│   │               │   │   └── RequestLoggingFilter.java
│   │               │   ├── response/
│   │               │       └── ErrorResponse.java
│   │               ├── listener/
│   │               │   ├── NotificationEventListener.java
│   │               │   └── UserActivityEventListener.java
│   │               ├── mapper/
│   │               │   ├── ArticleMapper.java
│   │               │   ├── ArticleViewMapper.java
│   │               │   ├── CommentMapper.java
│   │               │   ├── InterestMapper.java
│   │               │   ├── NotificationMapper.java
│   │               │   ├── UserActivityMapper.java
│   │               │   └── UserMapper.java
│   │               ├── monitoring/
│   │               │   └── BatchMetrics.java
│   │               ├── repository/
│   │               │   ├── impl/
│   │               │   │   ├── InterestRepositoryImpl.java
│   │               │   │   ├── NewsArticleRepositoryImpl.java
│   │               │   │   └── UserActivityRepositoryImpl.java
│   │               │   ├── ArticleBackupJobRepository.java
│   │               │   ├── ArticleInterestRepository.java
│   │               │   ├── ArticleViewRepository.java
│   │               │   ├── CommentLikeRepository.java
│   │               │   ├── CommentRepository.java
│   │               │   ├── InterestKeywordRepository.java
│   │               │   ├── InterestRepository.java
│   │               │   ├── InterestRepositoryCustom.java
│   │               │   ├── NewsArticleRepository.java
│   │               │   ├── NewsArticleRepositoryCustom.java
│   │               │   ├── NewsSourceRepository.java
│   │               │   ├── NotificationRepository.java
│   │               │   ├── SubscriptionRepository.java
│   │               │   ├── UserActivityRepository.java
│   │               │   ├── UserActivityRepositoryCustom.java
│   │               │   └── UserRepository.java
│   │               ├── service/
│   │               │   ├── ArticleBackupJobLogService.java
│   │               │   ├── ArticleBatchService.java
│   │               │   ├── ArticleService.java
│   │               │   ├── ArticleViewService.java
│   │               │   ├── CommentService.java
│   │               │   ├── InterestService.java
│   │               │   ├── NewsCollectService.java
│   │               │   ├── NewsSaveService.java
│   │               │   ├── NotificationService.java
│   │               │   ├── UserActivityService.java
│   │               │   └── UserService.java
│   │               ├── testdata/
│   │               │   ├── generator/
│   │               │   │   ├── AbstractGenerator.java
│   │               │   │   ├── CommentGenerator.java
│   │               │   │   ├── InterestGenerator.java
│   │               │   │   ├── NewsArticleGenerator.java
│   │               │   │   ├── NotificationGenerator.java
│   │               │   │   └── UserGenerator.java
│   │               │   ├── DataGenerationRunner.java
│   │               │   └── DataGeneratorConfig.java
│   │               └── MonewApplication.java
│   ├── resources/
│       ├── sql/
│       │   ├── h2/
│       │   │   ├── batch-schema.sql
│       │   │   └── schema-h2.sql
│       │   ├── postgresql/
│       │       ├── batch-schema.sql
│       │       └── schema-postgresql.sql
│       ├── static/
│       │   ├── assets/
│       │   │   ├── index-BBLciFoK.js
│       │   │   ├── index-CHX_5t7G.css
│       │   │   ├── landing_comments-BoMt6RvV.svg
│       │   │   ├── landing_interests-CBQzCgwG.svg
│       │   │   └── landing_notifications-BkwzqdfE.svg
│       │   ├── fonts/
│       │   │   ├── pretendard/
│       │   │       ├── LICENSE.txt
│       │   │       ├── Pretendard-Bold.woff2
│       │   │       ├── Pretendard-Regular.woff2
│       │   │       └── PretendardVariable.woff2
│       │   ├── favicon.ico
│       │   └── index.html
│       ├── templates/
│       ├── application-data-gen.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       ├── application-test.yml
│       ├── application.yml
│       └── logback-spring.xml
├── test/
    ├── java/
        ├── com/
            ├── team3/
                ├── monew/
                    ├── batch/
                    │   ├── job/
                    │   │   ├── ArticleBackupBatchConfigTest.java
                    │   │   └── ArticleCollectBatchConfigTest.java
                    │   ├── scheduler/
                    │   │   ├── ArticleBackupBatchScheduleConfigTest.java
                    │   │   ├── ArticleCollectBatchScheduleConfigTest.java
                    │   │   ├── NotificationDeleteScheduleConfigTest.java
                    │   │   └── UserDeleteSchedulerTest.java
                    │   ├── tasklet/
                    │   │   └── UserDeleteTaskletTest.java
                    │   ├── ArticleBackupBatchIntegrationTest.java
                    │   └── NotificationDeleteBatchIntegrationTest.java
                    ├── component/
                    │   ├── news/
                    │       ├── client/
                    │       │   ├── ChosunNewsClientTest.java
                    │       │   └── NaverNewsClientTest.java
                    │       ├── collect/
                    │       │   ├── ChosunNewsCollectTest.java
                    │       │   ├── NaverNewsCollectTest.java
                    │       │   └── NewsCollectorTest.java
                    │       ├── filter/
                    │       │   ├── KeywordMatchTest.java
                    │       │   └── NewsFilterTest.java
                    │       ├── parse/
                    │           ├── ChosunNewsParseTest.java
                    │           ├── NaverNewsParseTest.java
                    │           └── NewsParserTest.java
                    ├── config/
                    │   ├── BatchTestConfig.java
                    │   └── TestcontainersConfig.java
                    ├── controller/
                    │   ├── ArticleControllerTest.java
                    │   ├── ArticleViewControllerTest.java
                    │   ├── CommentControllerTest.java
                    │   ├── InterestControllerTest.java
                    │   ├── NotificationControllerTest.java
                    │   ├── UserActivityControllerTest.java
                    │   └── UserControllerTest.java
                    ├── integration/
                    │   ├── ArticleServiceIntegrationTest.java
                    │   ├── ArticleSourceIntegrationTest.java
                    │   ├── ArticleViewIntegrationTest.java
                    │   ├── CommentIntegrationTest.java
                    │   ├── InterestServiceIntegrationTest.java
                    │   ├── NewsCollectIntegrationTest.java
                    │   ├── NotificationIntegrationTest.java
                    │   ├── UserActivityIntegrationTest.java
                    │   └── UserIntegrationTest.java
                    ├── listener/
                    │   ├── NotificationEventListenerIntegrationTest.java
                    │   ├── NotificationEventListenerTest.java
                    │   └── UserActivityEventListenerTest.java
                    ├── repository/
                    │   ├── impl/
                    │   │   ├── InterestRepositoryTest.java
                    │   │   └── NewsArticleRepositoryImplTest.java
                    │   └── NotificationRepositoryTest.java
                    ├── service/
                    │   ├── ArticleBackupJobLogServiceTest.java
                    │   ├── ArticleBatchServiceTest.java
                    │   ├── ArticleServiceTest.java
                    │   ├── ArticleViewServiceTest.java
                    │   ├── CommentServiceTest.java
                    │   ├── InterestServiceTest.java
                    │   ├── NewsCollectServiceTest.java
                    │   ├── NewsSaveServiceTest.java
                    │   ├── NotificationServiceTest.java
                    │   ├── UserActivityServiceTest.java
                    │   └── UserServiceTest.java
                    ├── support/
                    │   └── IntegrationTestSupport.java
                    └── MonewApplicationTests.java

```

  </div>
</details>

## 🏠 **구현 홈페이지**

<a href="http://52.79.234.93" target="_blank">
  <img src="https://github.com/user-attachments/assets/a31d8ae2-f334-4db6-aeac-96af12b8d4db" width="600px" alt="Monew Image" />
</a>

> **이미지를 클릭**하면 서비스 페이지로 연결됩니다.


---

## 📝 **프로젝트 개인 개발 리포트**

- [김민형](개인개발 레포트 주소 추가)
- [김하은](https://www.notion.so/359cef5b940680eb9fdfcf48f548f5a8?source=copy_link)
- [김현민]()
- [조성진]()
- [황준수]()
