-- 任务优先级字段（可空）：HIGH / MEDIUM / LOW，NULL＝未设置。
ALTER TABLE taskify_task
    ADD COLUMN priority VARCHAR(8);
