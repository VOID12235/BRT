-- BTR 赛题附件 + 公示时间迁移
ALTER TABLE contest ADD COLUMN published_at datetime NULL DEFAULT NULL COMMENT '公示开始时间' AFTER finalized_at;
ALTER TABLE contest ADD COLUMN topic_file_name varchar(255) NULL DEFAULT NULL COMMENT '赛题文件名' AFTER video_name;
ALTER TABLE contest ADD COLUMN topic_file_url varchar(512) NULL DEFAULT NULL COMMENT '赛题文件地址' AFTER topic_file_name;
-- 已公示赛事补齐公示开始时间（优先用固化时间，无则用当前时间，保证"公示一周后自动结束"可触发）
UPDATE contest SET published_at = COALESCE(finalized_at, NOW()) WHERE status = 'PUBLISHED' AND published_at IS NULL;
