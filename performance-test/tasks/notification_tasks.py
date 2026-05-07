import random
from locust import TaskSet, task

class NotificationTasks(TaskSet):

    def on_start(self):
        # 이미 정의되어 있음
        pass

    def pick_new_user(self):
        """알림이 없는 유저를 만났을 때 호출하여 다른 유저 ID로 교체"""
        from locustfile import user_id_pool # locustfile에 있는 풀을 참조
        self.user_id = random.choice(user_id_pool)
        # 부모 클래스의 user_id도 업데이트 (get_headers에서 사용하므로)
        self.parent.user_id = self.user_id

    @task #항상 알림 조회 후 확인
    def notification_workflow(self):
        headers = self.parent.get_headers()

        with self.client.get("/api/notifications",
                             headers=headers,
                             params={"limit": 50},#실제 프론트 요청 세팅
                             name="/api/notifications (List-50)",
                             catch_response=True) as response:

            if response.status_code == 200:
                try:
                    data = response.json()
                    unread_list = data.get("content", [])

                    if unread_list:
                        action = random.random()
                        if action < 0.7: # 전체 확인(대부분)
                            self.client.patch("/api/notifications",
                                              headers=headers,
                                              name="/api/notifications (All)")
                        else: # 개별 확인
                            target_id = random.choice(unread_list).get("id")
                            self.client.patch(f"/api/notifications/{target_id}",
                                              headers=headers,
                                              name="/api/notifications/{id}")
                    else:
                        # 읽을 알림이 없으면 새 유저로 교체
                        self.pick_new_user()
                except Exception:
                    # JSON 파싱 에러 등에 대비해 실패 처리
                    response.failure("Failed to parse JSON response")
            else:
                # 200이 아닌 경우 실패로 기록
                response.failure(f"Got status code {response.status_code}")

    @task(1)
    def stop(self):
        self.interrupt()