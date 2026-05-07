import random
import uuid
from locust import TaskSet, task


class CommentTasks(TaskSet):

    def _get_random_article_id(self):
        article_id_pool = getattr(self.parent, "article_id_pool", None)
        if article_id_pool:
            return random.choice(article_id_pool)

        # articles.csv 연결 전까지는 기본값 사용
        return "00000000-0000-0000-0000-000000000000"

    # 댓글 목록 조회
    @task(3)
    def get_comments(self):
        params = {
            "articleId": self._get_random_article_id(),
            "orderBy": "createdAt",
            "direction": "DESC",
            "limit": 20,
        }
        self.client.get(
            "/api/comments",
            headers=self.parent.get_headers(),
            params=params,
        )

    # 댓글 등록
    @task(1)
    def create_comment(self):
        payload = {
            "articleId": self._get_random_article_id(),
            "userId": self.parent.user_id,
            "content": f"locust-comment-{uuid.uuid4()}",
        }
        self.client.post(
            "/api/comments",
            json=payload,
            headers=self.parent.get_headers(),
        )

    @task(1)
    def stop(self):
        self.interrupt()
