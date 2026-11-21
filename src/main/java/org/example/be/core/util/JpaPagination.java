package org.example.be.core.util;

import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class JpaPagination {

    private JpaPagination() {
    }

    public static PageRequest request(int page, int pageSize, Sort sort) {
        if ((page - 1L) * pageSize > Integer.MAX_VALUE) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
        return PageRequest.of(page - 1, pageSize, sort);
    }
}
