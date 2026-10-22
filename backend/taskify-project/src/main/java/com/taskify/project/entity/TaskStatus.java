package com.taskify.project.entity;

/**
 * 看板四列（任务状态）及其相邻关系。存储为字符串（TODO/IN_PROGRESS/IN_REVIEW/DONE），
 * 「相邻列」即枚举顺序上的相邻（仅允许前进或后退一步）。
 */
public enum TaskStatus {
    TODO("待办事项", 0),
    IN_PROGRESS("进行中", 1),
    IN_REVIEW("审查中", 2),
    DONE("已完成", 3);

    private final String label;
    private final int order;

    TaskStatus(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String label() {
        return label;
    }

    public int order() {
        return order;
    }

    /** 解析字符串；非法值返回 null。 */
    public static TaskStatus from(String value) {
        if (value == null) {
            return null;
        }
        for (TaskStatus s : values()) {
            if (s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        return null;
    }

    /** 两状态是否相邻（仅允许前进或后退一步）。 */
    public static boolean isAdjacent(TaskStatus a, TaskStatus b) {
        return a != null && b != null && Math.abs(a.order - b.order) == 1;
    }
}
