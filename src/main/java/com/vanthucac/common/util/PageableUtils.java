package com.vanthucac.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageableUtils {

    private PageableUtils() {
    }

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "price", "title",
            "startTime", "endTime", "amount", "currentPrice", "startingPrice"
    );

    public static PageRequest build(int page, int size, String sort) {
        var safePage = Math.max(0, page);
        var safeSize = (size <= 0 || size > MAX_PAGE_SIZE) ? DEFAULT_PAGE_SIZE : size;

        if (sort == null || sort.isBlank()) {
            return PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        }

        var parts = sort.split(",");
        var field = parts[0].trim();

        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            return PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        }

        var direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }
}