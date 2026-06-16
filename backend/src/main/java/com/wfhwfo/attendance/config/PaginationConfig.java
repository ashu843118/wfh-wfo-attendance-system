package com.wfhwfo.attendance.config;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PaginationConfig implements PageableHandlerMethodArgumentResolverCustomizer {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;

    @Override
    public void customize(org.springframework.data.web.PageableHandlerMethodArgumentResolver resolver) {
        resolver.setMaxPageSize(MAX_PAGE_SIZE);
        resolver.setFallbackPageable(PageRequest.of(0, DEFAULT_PAGE_SIZE));
    }

    @Schema(description = "Page index (0-based)", example = "0")
    public interface PageParam {
    }

    @Schema(description = "Page size (max 100)", example = "20")
    public interface SizeParam {
    }
}
