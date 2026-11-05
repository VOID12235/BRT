-- BTR 赛事平台 · 存量数据库升级脚本（MySQL 8）
-- 适用于由旧版 deploy/mysql/init.sql 初始化的数据库。
-- 脚本可重复执行：已存在的列 / 索引会自动跳过。

USE btr;

DROP PROCEDURE IF EXISTS btr_add_column;
DROP PROCEDURE IF EXISTS btr_add_index;

DELIMITER $$

CREATE PROCEDURE btr_add_column(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col
    ) THEN
        SET @btr_ddl = ddl;
        PREPARE btr_stmt FROM @btr_ddl;
        EXECUTE btr_stmt;
        DEALLOCATE PREPARE btr_stmt;
    END IF;
END $$

CREATE PROCEDURE btr_add_index(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND INDEX_NAME = idx
    ) THEN
        SET @btr_ddl = ddl;
        PREPARE btr_stmt FROM @btr_ddl;
        EXECUTE btr_stmt;
        DEALLOCATE PREPARE btr_stmt;
    END IF;
END $$

DELIMITER ;

-- 1. Token 版本号：改密 / 禁用 / 角色变更后使旧 Token 失效
CALL btr_add_column('sys_user', 'token_version',
    'ALTER TABLE sys_user ADD COLUMN token_version INT NOT NULL DEFAULT 0');

-- 2. 奖项奖品字段
CALL btr_add_column('contest_award', 'gift',
    'ALTER TABLE contest_award ADD COLUMN gift VARCHAR(255)');

-- 3. 公告已读表
CREATE TABLE IF NOT EXISTS notice_read (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notice_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notice_read UNIQUE (notice_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 常用查询索引
CALL btr_add_index('contest', 'idx_contest_status',
    'CREATE INDEX idx_contest_status ON contest (status)');
CALL btr_add_index('contest_signup', 'idx_signup_contest',
    'CREATE INDEX idx_signup_contest ON contest_signup (contest_id)');
CALL btr_add_index('contest_work', 'idx_work_contest',
    'CREATE INDEX idx_work_contest ON contest_work (contest_id)');
CALL btr_add_index('contest_judge_assign', 'idx_assign_contest',
    'CREATE INDEX idx_assign_contest ON contest_judge_assign (contest_id)');
CALL btr_add_index('contest_score', 'idx_score_contest',
    'CREATE INDEX idx_score_contest ON contest_score (contest_id)');
CALL btr_add_index('contest_award', 'idx_award_contest',
    'CREATE INDEX idx_award_contest ON contest_award (contest_id)');
CALL btr_add_index('contest_winner', 'idx_winner_contest',
    'CREATE INDEX idx_winner_contest ON contest_winner (contest_id)');
CALL btr_add_index('notice_read', 'idx_notice_read_notice',
    'CREATE INDEX idx_notice_read_notice ON notice_read (notice_id)');

DROP PROCEDURE IF EXISTS btr_add_column;
DROP PROCEDURE IF EXISTS btr_add_index;

SELECT 'BTR 数据库升级完成' AS result;
