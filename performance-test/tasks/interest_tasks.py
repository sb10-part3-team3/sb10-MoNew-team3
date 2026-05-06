from locust import TaskSet, task
import random


class InterestTasks(TaskSet):

  @task(5)
  def get_interests_first_page(self):
    self.client.get(
        "/api/interests",
        headers=self.user.get_headers(),
        params={
          "orderBy": random.choice(["name", "subscriberCount"]),
          "direction": random.choice(["ASC", "DESC"]),
          "limit": 20
        }
    )

    @task(3)
    def get_sorted(self):
      self.client.get(
          "/api/interests",
          headers=self.user.get_headers(),
          params={
            "orderBy": "subscriberCount",
            "direction": "DESC",
            "limit": 10
          }
      )

  @task(3)
  def search_interests(self):
    self.client.get(
        "/api/interests",
        headers=self.user.get_headers(),
        params={
          "keyword": random.choice(["주식", "경제", "스포츠", "AI"]),
          "orderBy": "name",
          "direction": "ASC",
          "limit": 20
        }
    )
