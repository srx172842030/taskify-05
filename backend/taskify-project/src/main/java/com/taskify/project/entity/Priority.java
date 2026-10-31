package com.taskify.project.entity;

/**
 * 任务优先级（可空）：HIGH / MEDIUM / LOW，空值＝未设置。
 * 存储为字符串；非法值解析返回 null。
 */
public enum Priority {
    HIGH("高"),
    MEDIUM("中"),
    LOW("低");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 解析字符串；非法值返回 null。 */
    public static Priority from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (Priority p : values()) {
            if (p.name().equalsIgnoreCase(value)) {
                return p;
            }
        }
        return null;
    }
}
