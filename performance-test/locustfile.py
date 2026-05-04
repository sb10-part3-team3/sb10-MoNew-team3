import csv
import random
from pathlib import Path
from locust import HttpUser, between
from tasks.notification_tasks import NotificationTasks
#from tasks.파일명 import 임포트명 형식으로 작성

# 테스트 유저 리스트
user_id_pool = []

# 테스트 시작전 유저 목록 불러오기
try:
    user_csv_path = Path(__file__).resolve().parent / "data" / "users.csv"
    with user_csv_path.open(newline="", encoding="utf-8") as f:
        reader = csv.reader(f)
        user_id_pool = [row[0].strip() for row in reader if row and row[0].strip()]
    if not user_id_pool:
            print("⚠️ data/users.csv 파일이 비어 있습니다. 기본 ID를 사용합니다.")
            user_id_pool = ["00000000-0000-0000-0000-000000000000"]
except FileNotFoundError:
    print("❌ data/users.csv 파일을 찾을 수 없습니다.")
    user_id_pool = ["00000000-0000-0000-0000-000000000000"]

# 공통 헤더를 위한 클래스
class MonewUser(HttpUser):
    abstract = True
    wait_time = between(1, 2)
    host = "http://localhost:8080"

    def on_start(self):
        # 유저마다 고유 ID 할당 (csv 랜덤 추출)
        self.user_id = random.choice(user_id_pool)

    def get_headers(self):
        return {"Monew-Request-User-ID": self.user_id}

# 실행할 태스크 조합
class WebsiteUser(MonewUser):
    tasks = {
        NotificationTasks: 1,
        # 아래에 다른 도메인 태스크 추가
        # CommentTasks: 2
    }