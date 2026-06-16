package com.wfhwfo.attendance.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public final class PagedResponseMapper {

    private PagedResponseMapper() {
    }

    public static <T> PagedResponse<T> from(Page<T> page) {
        return from(page, Function.identity());
    }

    public static <T, R> PagedResponse<R> from(Page<T> page, Function<T, R> mapper) {
        List<R> content = page.getContent().stream().map(mapper).toList();
        return PagedResponse.<R>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
