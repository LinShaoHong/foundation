package com.github.sun.foundation.boot.exception;

public class BadRequestException extends ResponsiveException {
    private static final long serialVersionUID = -5830214050353226720L;

    private static final Kind k = Kind.BAD_REQUEST;

    public BadRequestException(int code) {
        super(code, null, k);
    }

    public BadRequestException(String message) {
        super(getStatus(k).getStatusCode(), message, k);
    }

    public BadRequestException(int code, String message) {
        super(code, message, k);
    }
}
