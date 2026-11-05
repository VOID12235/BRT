-- BTR 评委分配满血版：数据模型扩展
ALTER TABLE contest
  ADD COLUMN judge_per_work INT DEFAULT 3,
  ADD COLUMN aggregate_mode VARCHAR(10) DEFAULT 'AVG',
  ADD COLUMN review_deadline DATETIME NULL,
  ADD COLUMN results_locked TINYINT DEFAULT 0,
  ADD COLUMN finalized_at DATETIME NULL;

ALTER TABLE contest_judge_assign
  ADD COLUMN assign_type VARCHAR(10) DEFAULT 'ITEM';

ALTER TABLE contest_score
  ADD COLUMN adjusted_score DECIMAL(5,1) NULL,
  ADD COLUMN adjust_reason VARCHAR(500) DEFAULT '',
  ADD COLUMN adjust_by BIGINT NULL,
  ADD COLUMN adjusted_at DATETIME NULL;

-- 历史整体分配记录标记为 ALL（work_id 为空的旧记录按 ITEM 保留，动态跟随只对新分配生效）
UPDATE contest_judge_assign SET assign_type='ITEM' WHERE assign_type='ITEM' OR assign_type IS NULL;
