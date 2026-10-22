-- 任务时间字段 + 提醒节点 + 阻塞关系（依赖关系）。
ALTER TABLE taskify_task
    ADD COLUMN start_at      TIMESTAMPTZ,
    ADD COLUMN start_all_day BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN due_at        TIMESTAMPTZ,
    ADD COLUMN due_all_day   BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE taskify_reminder (
    id             VARCHAR(36)  PRIMARY KEY,
    task_id        VARCHAR(36)  NOT NULL,
    mode           VARCHAR(16)  NOT NULL,
    trigger_at     TIMESTAMPTZ,
    offset_minutes INT,
    anchor         VARCHAR(8),
    fired          BOOLEAN      NOT NULL DEFAULT false,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE taskify_task_dependency (
    id           VARCHAR(36)  PRIMARY KEY,
    blocker_id   VARCHAR(36)  NOT NULL,
    dependent_id VARCHAR(36)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_task_dependency UNIQUE (blocker_id, dependent_id)
);

CREATE INDEX idx_taskify_reminder_task ON taskify_reminder (task_id);
CREATE INDEX idx_taskify_dependency_blocker ON taskify_task_dependency (blocker_id);
CREATE INDEX idx_taskify_dependency_dependent ON taskify_task_dependency (dependent_id);
