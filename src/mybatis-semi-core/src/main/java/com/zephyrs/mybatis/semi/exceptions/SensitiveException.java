package com.zephyrs.mybatis.semi.exceptions;

public class SensitiveException extends RuntimeException {

    public SensitiveException(String message) {
        super(message);
    }

    public SensitiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
