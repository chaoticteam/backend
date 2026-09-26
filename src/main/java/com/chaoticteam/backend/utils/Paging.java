package com.chaoticteam.backend.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Paging {

    private Paging() {
    }

    /** A null or non-positive limit means "no limit" (go-server uses -1). */
    public static Pageable of(Integer limit, Sort sort) {
        if (limit == null || limit <= 0) {
            return Pageable.unpaged(sort);
        }
        return PageRequest.of(0, limit, sort);
    }
}
