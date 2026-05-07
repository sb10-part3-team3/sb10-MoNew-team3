import csv
import random
from pathlib import Path

from locust import HttpUser, between

from tasks.comment_tasks import CommentTasks
from tasks.interest_tasks import InterestTasks
from tasks.notification_tasks import NotificationTasks

# Load test id pools
user_id_pool = []
article_id_pool = []

# Load users.csv before test start
try:
  user_csv_path = Path(__file__).resolve().parent / "data" / "users.csv"
  with user_csv_path.open(newline="", encoding="utf-8") as f:
    reader = csv.reader(f)
    user_id_pool = [row[0].strip() for row in reader if row and row[0].strip()]
  if not user_id_pool:
    print("WARNING: data/users.csv is empty. Using fallback user id.")
    user_id_pool = ["00000000-0000-0000-0000-000000000000"]
except FileNotFoundError:
  print("ERROR: data/users.csv not found.")
  user_id_pool = ["00000000-0000-0000-0000-000000000000"]

# Load articles.csv before test start
try:
  article_csv_path = Path(__file__).resolve().parent / "data" / "articles.csv"
  with article_csv_path.open(newline="", encoding="utf-8") as f:
    reader = csv.reader(f)
    article_id_pool = [row[0].strip() for row in reader if row and row[0].strip()]
  if not article_id_pool:
    print("WARNING: data/articles.csv is empty. Using fallback article id.")
    article_id_pool = ["00000000-0000-0000-0000-000000000000"]
except FileNotFoundError:
  print("ERROR: data/articles.csv not found.")
  article_id_pool = ["00000000-0000-0000-0000-000000000000"]


class MonewUser(HttpUser):
  abstract = True
  wait_time = between(1, 2)
  host = "http://localhost:8080"

  def on_start(self):
    self.user_id = random.choice(user_id_pool)
    self.article_id_pool = article_id_pool

  def get_headers(self):
    return {"Monew-Request-User-ID": self.user_id}


class WebsiteUser(MonewUser):
  tasks = {
    NotificationTasks: 1,
    InterestTasks: 1,
    CommentTasks: 1,
  }
