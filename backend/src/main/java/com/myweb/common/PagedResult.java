package com.myweb.common;

import java.util.List;

public class PagedResult<T> {
    private final List<T> items;
    private final long total;
    private final int page;
    private final int limit;

    public PagedResult(List<T> items, long total, int page, int limit) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.limit = limit;
    }

    public List<T> getItems() {
        return items;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getLimit() {
        return limit;
    }
}

