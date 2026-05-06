-- ═══════════════════════════════════════════════════════════════
--  AP_Code Platform — PostgreSQL Schema
--  Run this ONCE to create the database before starting the app.
--  Spring Boot with ddl-auto=update will handle incremental changes.
-- ═══════════════════════════════════════════════════════════════

-- Create the database (run as postgres superuser)
CREATE DATABASE apcode_db
    WITH ENCODING 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE   = 'en_US.UTF-8'
    TEMPLATE = template0;

-- Connect to it: \c apcode_db

-- Create a dedicated app user (recommended for production)
CREATE USER apcode_user WITH PASSWORD 'strong_password_here';
GRANT ALL PRIVILEGES ON DATABASE apcode_db TO apcode_user;
GRANT ALL ON SCHEMA public TO apcode_user;

-- ─── Tables (Spring JPA will auto-create, but here for reference) ─

CREATE TABLE IF NOT EXISTS users (
    id                   BIGSERIAL PRIMARY KEY,
    full_name            VARCHAR(100)        NOT NULL,
    email                VARCHAR(255) UNIQUE NOT NULL,
    password             VARCHAR(255)        NOT NULL,
    role                 VARCHAR(20)         NOT NULL DEFAULT 'STUDENT',
    city                 VARCHAR(100),
    total_points         INT                 NOT NULL DEFAULT 0,
    current_streak_days  INT                 NOT NULL DEFAULT 0,
    last_active_date     TIMESTAMP,
    newsletter_subscribed BOOLEAN            NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP           NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_points ON users(total_points DESC);

CREATE TABLE IF NOT EXISTS courses (
    id              BIGSERIAL PRIMARY KEY,
    slug            VARCHAR(100) UNIQUE NOT NULL,
    title           VARCHAR(255)        NOT NULL,
    description     TEXT,
    icon            VARCHAR(20),
    badge           VARCHAR(50),
    total_lectures  INT                 NOT NULL DEFAULT 0,
    is_free         BOOLEAN             NOT NULL DEFAULT TRUE,
    is_published    BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP           NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS videos (
    id            BIGSERIAL PRIMARY KEY,
    youtube_id    VARCHAR(20)  NOT NULL,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    thumbnail_url VARCHAR(500),
    order_index   INT          NOT NULL DEFAULT 0,
    is_free       BOOLEAN      NOT NULL DEFAULT TRUE,
    course_id     BIGINT       NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_video_course ON videos(course_id, order_index);

CREATE TABLE IF NOT EXISTS course_enrollments (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id        BIGINT    NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    progress_percent INT       NOT NULL DEFAULT 0,
    enrolled_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    last_accessed_at TIMESTAMP,
    UNIQUE(user_id, course_id)
);

CREATE TABLE IF NOT EXISTS completed_videos (
    enrollment_id BIGINT NOT NULL REFERENCES course_enrollments(id) ON DELETE CASCADE,
    video_id      BIGINT NOT NULL,
    PRIMARY KEY(enrollment_id, video_id)
);

CREATE TABLE IF NOT EXISTS newsletter_subscribers (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) UNIQUE NOT NULL,
    active        BOOLEAN             NOT NULL DEFAULT TRUE,
    subscribed_at TIMESTAMP           NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS reviews (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   BIGINT  REFERENCES courses(id) ON DELETE SET NULL,
    review_text TEXT    NOT NULL,
    rating      INT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    approved    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Helpful views
CREATE OR REPLACE VIEW v_platform_stats AS
SELECT
    (SELECT COUNT(DISTINCT user_id) FROM course_enrollments) AS total_students,
    (SELECT COUNT(*) FROM videos)                            AS total_videos,
    (SELECT COUNT(*) FROM courses WHERE is_published = TRUE) AS total_courses,
    (SELECT COUNT(*) FROM newsletter_subscribers WHERE active = TRUE) AS total_subscribers,
    (SELECT ROUND(AVG(rating)::numeric, 1) FROM reviews WHERE approved = TRUE) AS avg_rating;

CREATE OR REPLACE VIEW v_leaderboard AS
SELECT
    ROW_NUMBER() OVER (ORDER BY total_points DESC) AS rank,
    id, full_name, city, total_points, current_streak_days
FROM users
ORDER BY total_points DESC;
