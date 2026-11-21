package org.example.be.core.exception;

import lombok.Getter;

@Getter
public class BusinessLogicException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object details;

    public BusinessLogicException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public BusinessLogicException(ErrorCode errorCode, Object details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

}
