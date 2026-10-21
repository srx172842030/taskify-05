package com.taskify.common.response;

import lombok.Getter;

import java.util.List;

/**
 * 统一页码分页结果（items/page/size/total/totalPages）。
 */
@Getter
public class PageResult<T> {

    private final List<T> items;
    private final long page;
    private final long size;
    private final long total;
    private final long totalPages;

    public PageResult(List<T> items, long page, long size, long total) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = size <= 0 ? 0 : (total + size - 1) / size;
    }

    public static <T> PageResult<T> of(List<T> items, long page, long size, long total) {
        return new PageResult<>(items, page, size, total);
    }
}
