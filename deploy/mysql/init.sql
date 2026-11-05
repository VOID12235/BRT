-- BTR 赛事平台 · MySQL 8 初始化脚本
-- 由 src/main/resources/schema-mysql.sql 同步生成，docker-compose 首次启动时自动执行
-- 注意：本脚本会重建所有表，仅用于全新数据库；存量库升级请使用 migrate.sql
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS btr DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE btr;
-- BTR 璧涗簨骞冲彴 路 MySQL 8 寤鸿〃鑴氭湰锛堜笌 schema-h2.sql 缁撴瀯绛変环锛?
DROP TABLE IF EXISTS notice_read;
DROP TABLE IF EXISTS sys_log;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_email_record;
DROP TABLE IF EXISTS upload_file;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS contest_winner;
DROP TABLE IF EXISTS contest_award;
DROP TABLE IF EXISTS contest_score;
DROP TABLE IF EXISTS contest_judge_assign;
DROP TABLE IF EXISTS contest_work;
DROP TABLE IF EXISTS contest_signup;
DROP TABLE IF EXISTS contest;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account VARCHAR(64) NOT NULL,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    email VARCHAR(128),
    phone VARCHAR(32),
    role VARCHAR(20) NOT NULL DEFAULT 'PLAYER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    avatar VARCHAR(255),
    token_version INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_account UNIQUE (account),
    CONSTRAINT uk_user_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contest (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    cover VARCHAR(255),
    summary VARCHAR(500),
    description TEXT,
    rules TEXT,
    prize_amount DECIMAL(14,2) DEFAULT 0.00,
    mode VARCHAR(10) NOT NULL DEFAULT 'ONLINE',
    signup_start DATETIME,
    signup_end DATETIME,
    review_start DATETIME,
    review_end DATETIME,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_contest_status ON contest (status);

CREATE TABLE contest_signup (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contest_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    school VARCHAR(128),
    contact VARCHAR(64),
    intro VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_signup_user_contest UNIQUE (user_id, contest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_signup_contest ON contest_signup (contest_id);

CREATE TABLE contest_work (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    signup_id BIGINT NOT NULL,
    contest_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    file_url VARCHAR(500),
    file_name VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_work_signup UNIQUE (signup_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_work_contest ON contest_work (contest_id);

CREATE TABLE contest_judge_assign (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contest_id BIGINT NOT NULL,
    judge_id BIGINT NOT NULL,
    work_id BIGINT,
    signup_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_assign_judge_work UNIQUE (judge_id, work_id, signup_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_assign_contest ON contest_judge_assign (contest_id);

CREATE TABLE contest_score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contest_id BIGINT NOT NULL,
    work_id BIGINT,
    signup_id BIGINT,
    judge_id BIGINT NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    comment VARCHAR(1000),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_score_work_judge UNIQUE (work_id, judge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_score_contest ON contest_score (contest_id);

CREATE TABLE contest_award (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contest_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    quota INT NOT NULL DEFAULT 1,
    prize_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    gift VARCHAR(255),
    sort_order INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_award_contest ON contest_award (contest_id);

CREATE TABLE contest_winner (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contest_id BIGINT NOT NULL,
    award_id BIGINT NOT NULL,
    work_id BIGINT,
    signup_id BIGINT,
    rank_no INT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_winner_contest ON contest_winner (contest_id);

CREATE TABLE notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    content TEXT,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    published_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE upload_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(500) NOT NULL,
    md5 VARCHAR(64),
    size BIGINT,
    original_name VARCHAR(255),
    uploader_id BIGINT,
    purpose VARCHAR(32),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_file_md5 UNIQUE (md5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_email_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient VARCHAR(128) NOT NULL,
    template VARCHAR(64),
    subject VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(64) NOT NULL,
    config_value VARCHAR(2000),
    description VARCHAR(255),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_config_key UNIQUE (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(64),
    target VARCHAR(255),
    ip VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notice_read (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notice_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notice_read UNIQUE (notice_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_notice_read_notice ON notice_read (notice_id);
