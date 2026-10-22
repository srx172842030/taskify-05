-- 通知表。read 列在 PostgreSQL 中非保留字，可作列名。
CREATE TABLE taskify_notification (
    id           VARCHAR(36)  PRIMARY KEY,
    recipient_id VARCHAR(36)  NOT NULL,
    type         VARCHAR(32)  NOT NULL,
    task_id      VARCHAR(36),
    message      VARCHAR(500) NOT NULL,
    read         BOOLEAN      NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_taskify_notification_recipient ON taskify_notification (recipient_id);
