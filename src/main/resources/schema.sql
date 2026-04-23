-- =========================
-- ENUMS
-- =========================

-- CREATE TYPE delete_statuses AS ENUM (
--     'ACTIVE',
--     'DELETED'
-- );
--
-- CREATE TYPE notification_resource_types AS ENUM (
--     'INTEREST',
--     'COMMENT'
-- );
--
-- CREATE TYPE backup_job_types AS ENUM (
--     'ARTICLE_DAILY_BACKUP',
--     'ARTICLE_RESTORE'
-- );
--
-- CREATE TYPE backup_job_statuses AS ENUM (
--     'PENDING',
--     'SUCCESS',
--     'FAILED'
-- );

-- CREATE TYPE news_source_types AS ENUM (
--     'NAVER',
--     'CHOSUN'
-- );

-- =========================
-- USERS
-- =========================

CREATE TABLE users
(
    id                 UUID PRIMARY KEY,
    email              VARCHAR(255)             NOT NULL UNIQUE,
    nickname           VARCHAR(100)             NOT NULL,
    password           VARCHAR(255)             NOT NULL,

    delete_status      VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE' CHECK (delete_status IN ('ACTIVE', 'DELETED')),
    deleted_at         TIMESTAMP WITH TIME ZONE,
    purge_scheduled_at TIMESTAMP WITH TIME ZONE,

    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL
);

-- =========================
-- INTERESTS
-- =========================

CREATE TABLE interests
(
    id               UUID PRIMARY KEY,
    name             VARCHAR(100)             NOT NULL UNIQUE,
    subscriber_count INT                      NOT NULL DEFAULT 0,

    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE interest_keywords
(
    id          UUID PRIMARY KEY,
    interest_id UUID                     NOT NULL,
    keyword     VARCHAR(100)             NOT NULL,

    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_interest_keywords_interest_id
        FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE,

    CONSTRAINT uk_interest_keywords_interest_id_keyword
        UNIQUE (interest_id, keyword)
);

-- =========================
-- SUBSCRIPTIONS
-- =========================

CREATE TABLE subscriptions
(
    id          UUID PRIMARY KEY,
    user_id     UUID                     NOT NULL,
    interest_id UUID                     NOT NULL,

    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_subscriptions_user_id
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT fk_subscriptions_interest_id
        FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE,

    CONSTRAINT uk_subscriptions_user_id_interest_id
        UNIQUE (user_id, interest_id)
);

-- =========================
-- NEWS SOURCES
-- =========================

CREATE TABLE news_sources
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(100)             NOT NULL UNIQUE,
    source_type VARCHAR(20)              NOT NULL,
    base_url    VARCHAR(500),

    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- =========================
-- NEWS ARTICLES
-- =========================

CREATE TABLE news_articles
(
    id            UUID PRIMARY KEY,
    source_id     UUID                     NOT NULL,
    original_link VARCHAR(1000)            NOT NULL UNIQUE,
    title         VARCHAR(500)             NOT NULL,
    published_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    summary       TEXT,

    comment_count INT                      NOT NULL DEFAULT 0,
    view_count    INT                      NOT NULL DEFAULT 0,

    delete_status VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE' CHECK (delete_status IN ('ACTIVE', 'DELETED')),
    deleted_at    TIMESTAMP WITH TIME ZONE,

    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_news_articles_source_id
        FOREIGN KEY (source_id) REFERENCES news_sources (id) ON DELETE RESTRICT
);

CREATE TABLE article_interests
(
    id              UUID PRIMARY KEY,
    article_id      UUID                     NOT NULL,
    interest_id     UUID                     NOT NULL,
    matched_keyword VARCHAR(100),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_article_interests_article_id
        FOREIGN KEY (article_id) REFERENCES news_articles (id) ON DELETE CASCADE,

    CONSTRAINT fk_article_interests_interest_id
        FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE CASCADE,

    CONSTRAINT uk_article_interests_article_id_interest_id
        UNIQUE (article_id, interest_id)
);

CREATE TABLE article_views
(
    id              UUID PRIMARY KEY,
    article_id      UUID                     NOT NULL,
    user_id         UUID                     NOT NULL,

    first_viewed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_viewed_at  TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_article_views_article_id
        FOREIGN KEY (article_id) REFERENCES news_articles (id) ON DELETE CASCADE,

    CONSTRAINT fk_article_views_user_id
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT uk_article_views_article_id_user_id
        UNIQUE (article_id, user_id)
);

-- =========================
-- COMMENTS
-- =========================

CREATE TABLE comments
(
    id            UUID PRIMARY KEY,
    article_id    UUID                     NOT NULL,
    user_id       UUID                     NOT NULL,
    content       VARCHAR(10000)           NOT NULL,

    like_count    INT                      NOT NULL DEFAULT 0,

    delete_status VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE' CHECK (delete_status IN ('ACTIVE', 'DELETED')),
    deleted_at    TIMESTAMP WITH TIME ZONE,

    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_comments_article_id
        FOREIGN KEY (article_id) REFERENCES news_articles (id) ON DELETE CASCADE,

    CONSTRAINT fk_comments_user_id
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE comment_likes
(
    id         UUID PRIMARY KEY,
    comment_id UUID                     NOT NULL,
    user_id    UUID                     NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_comment_likes_comment_id
        FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,

    CONSTRAINT fk_comment_likes_user_id
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT uk_comment_likes_comment_id_user_id
        UNIQUE (comment_id, user_id)
);

-- =========================
-- NOTIFICATIONS
-- =========================

CREATE TABLE notifications
(
    id            UUID PRIMARY KEY,
    user_id       UUID                     NOT NULL,

    content       VARCHAR(500)             NOT NULL,

    resource_type VARCHAR(30)              NOT NULL,
    resource_id   UUID                     NOT NULL,

    actor_user_id UUID,

    is_confirmed  BOOLEAN                  NOT NULL DEFAULT FALSE,
    confirmed_at  TIMESTAMP WITH TIME ZONE,

    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_notifications_user_id
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT fk_notifications_actor_user_id
        FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
);

-- =========================
-- ARTICLE BACKUP JOBS
-- =========================

CREATE TABLE article_backup_jobs
(
    id            UUID PRIMARY KEY,
    backup_date   DATE                     NOT NULL,
    job_type      VARCHAR(50)              NOT NULL,
    status        VARCHAR(20)              NOT NULL,

    s3_bucket     VARCHAR(255),
    s3_key        VARCHAR(500),
    article_count INT                      NOT NULL DEFAULT 0,
    started_at    TIMESTAMP WITH TIME ZONE,
    finished_at   TIMESTAMP WITH TIME ZONE,
    error_message TEXT,

    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

-- =========================
-- INDEXES
-- =========================

-- CREATE INDEX idx_users_delete_status
--     ON users(delete_status);
--
-- CREATE INDEX idx_users_purge_scheduled_at
--     ON users(delete_status, purge_scheduled_at);
--
-- CREATE INDEX idx_interest_keywords_keyword
--     ON interest_keywords(keyword);
--
-- CREATE INDEX idx_subscriptions_interest_id
--     ON subscriptions(interest_id);
--
-- CREATE INDEX idx_news_articles_published_at
--     ON news_articles(published_at);
--
-- CREATE INDEX idx_news_articles_view_count
--     ON news_articles(view_count);
--
-- CREATE INDEX idx_news_articles_comment_count
--     ON news_articles(comment_count);
--
-- CREATE INDEX idx_news_articles_delete_status
--     ON news_articles(delete_status);
--
-- CREATE INDEX idx_article_interests_interest_id
--     ON article_interests(interest_id);
--
-- CREATE INDEX idx_article_views_user_id
--     ON article_views(user_id);
--
-- CREATE INDEX idx_comments_article_id
--     ON comments(article_id);
--
-- CREATE INDEX idx_comments_created_at
--     ON comments(created_at);
--
-- CREATE INDEX idx_comments_like_count
--     ON comments(like_count);
--
-- CREATE INDEX idx_comments_delete_status
--     ON comments(delete_status);
--
-- CREATE INDEX idx_comment_likes_user_id
--     ON comment_likes(user_id);
--
-- CREATE INDEX idx_notifications_user_id
--     ON notifications(user_id);
--
-- CREATE INDEX idx_notifications_is_confirmed
--     ON notifications(is_confirmed);
--
-- CREATE INDEX idx_notifications_created_at
--     ON notifications(created_at);
--
-- CREATE INDEX idx_notifications_resource_type_resource_id
--     ON notifications(resource_type, resource_id);
--
-- CREATE INDEX idx_article_backup_jobs_backup_date
--     ON article_backup_jobs(backup_date);
--
-- CREATE INDEX idx_article_backup_jobs_backup_date_job_type
--     ON article_backup_jobs(backup_date, job_type);