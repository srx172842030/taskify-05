-- owner_id 为对 taskify_user.id 的逻辑引用（资源级授权在应用层校验属主身份）。
CREATE TABLE taskify_example (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    description TEXT,
    owner_id    VARCHAR(36)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_taskify_example_owner ON taskify_example (owner_id);
