from locust import TaskSet, task

class NotificationTasks(TaskSet):

    # 알림 목록 조회
    @task(3)
    def get_notifications(self):
        # 헤더 아이디 필수
        params = {"limit": 50}
        self.client.get("/api/notifications",
                        headers=self.parent.get_headers(),
                        params=params)
