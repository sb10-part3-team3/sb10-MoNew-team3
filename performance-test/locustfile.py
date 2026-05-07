import csv
import random
from pathlib import Path

from locust import HttpUser, between

from tasks.comment_tasks import CommentTasks
from tasks.interest_tasks import InterestTasks
from tasks.notification_tasks import NotificationTasks
from tasks.user_tasks import UserTasks

# Load test id pools
user_id_pool = []
user_email_pool = []
article_id_pool = []

# Load users.csv before test start
# users.csv 형식: user_id, email (2열)
# ex) 550e8400-e29b-41d4-a716-446655440000,user_a1b2c3d4@monew.com
try:
  user_csv_path = Path(__file__).resolve().parent / "data" / "users.csv"
  with user_csv_path.open(newline="", encoding="utf-8") as f:
    reader = csv.reader(f)
    for row in reader:
      if row and row[0].strip():
        user_id_pool.append(row[0].strip())
        user_email_pool.append(row[1].strip()) if len(row) > 1 else None
  if not user_id_pool:
    print("WARNING: data/users.csv is empty. Using fallback user id.")
    user_id_pool = ["00000000-0000-0000-0000-000000000000"]
    user_email_pool = ["user_00000000@monew.com"]
except FileNotFoundError:
  print("ERROR: data/users.csv not found.")
  user_id_pool = ["00000000-0000-0000-0000-000000000000"]
  user_email_pool = ["user_00000000@monew.com"]

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
    idx = random.randint(0, len(user_id_pool) - 1)
    self.user_id = user_id_pool[idx]
    self.user_email_pool = user_email_pool  # UserTasks에서 참조
    self.article_id_pool = article_id_pool

  def get_headers(self):
    return {"Monew-Request-User-ID": self.user_id}


class WebsiteUser(MonewUser):
  tasks = {
    NotificationTasks: 1,
    InterestTasks: 1,
    CommentTasks: 1,
    UserTasks: 1,
  }
