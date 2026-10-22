-- 项目/任务/评论三表。看板四列建模为任务的 status 枚举，不建独立列实体。
CREATE TABLE taskify_project (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    description TEXT,
    created_by  VARCHAR(36)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE taskify_task (
    id          VARCHAR(36)  PRIMARY KEY,
    project_id  VARCHAR(36)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    assignee_id VARCHAR(36),
    status      VARCHAR(32)  NOT NULL DEFAULT 'TODO',
    created_by  VARCHAR(36)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE taskify_comment (
    id          VARCHAR(36)  PRIMARY KEY,
    task_id     VARCHAR(36)  NOT NULL,
    author_id   VARCHAR(36)  NOT NULL,
    content     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_taskify_project_created_by ON taskify_project (created_by);
CREATE INDEX idx_taskify_task_project ON taskify_task (project_id);
CREATE INDEX idx_taskify_task_assignee ON taskify_task (assignee_id);
CREATE INDEX idx_taskify_comment_task ON taskify_comment (task_id);
