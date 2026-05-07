import random
import uuid
from locust import TaskSet, task


class UserTasks(TaskSet):

  # 로그인 - bcrypt 비교 병목 확인
  @task(7)
  def login(self):
    user_email_pool = getattr(self.parent, "user_email_pool", None)
    if not user_email_pool:
      return
    self.client.post(
      "/api/users/login",
      json={
        "email": random.choice(user_email_pool),
        "password": "TestPassword123!"
      },
      name="POST /api/users/login"
    )

  # 닉네임 수정 - 동시 수정 경합 확인
  @task(3)
  def update_nickname(self):
    self.client.patch(
      f"/api/users/{self.parent.user_id}",
      json={"nickname": f"nick_{random.randint(1000, 9999)}"},
      headers=self.parent.get_headers(),
      name="PATCH /api/users/{userId}"
    )

  # 회원가입 - 소량만 (bcrypt 인코딩 무거움)
  @task(1)
  def register(self):
    self.client.post(
      "/api/users",
      json={
        "email": f"loadtest_{uuid.uuid4().hex[:8]}@monew.com",
        "nickname": f"테스터{random.randint(1, 999)}",
        "password": "TestPassword123!"
      },
      name="POST /api/users"
    )

  @task(1)
  def stop(self):
    self.interrupt()