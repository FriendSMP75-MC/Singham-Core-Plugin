package com.friendsmp.singhamcore.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Pagination {
    private Pagination() {}

    public static <T> List<T> page(List<T> items, int page, int perPage) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        int total = items.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) perPage));
        int p = Math.max(1, Math.min(page, totalPages));
        int start = (p - 1) * perPage;
        int end = Math.min(start + perPage, total);
        return new ArrayList<>(items.subList(start, end));
    }
}
