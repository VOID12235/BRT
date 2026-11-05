-- 赛事状态机升级：新增「打分中」(JUDGING)
-- 存量「进行中」(REVIEW) 且已过评审开始时间的赛事 → 推进为「打分中」(JUDGING)，并锁定线上赛作品（评委开始评审，选手不可再改）
UPDATE contest SET status = 'JUDGING', updated_at = NOW()
WHERE status = 'REVIEW' AND review_start IS NOT NULL AND review_start <= NOW();

UPDATE work w JOIN contest c ON w.contest_id = c.id
SET w.locked = TRUE
WHERE c.status = 'JUDGING' AND c.mode = 'ONLINE';
