package com.github.sun.foundation.boot.exception;

public class AccessDeniedException extends ResponsiveException {
    private static final long serialVersionUID = -8638012246260564588L;

    private static final Kind k = Kind.ACCESS_DENIED;

    public AccessDeniedException(int code) {
        super(code, null, k);
    }

    public AccessDeniedException(String message) {
        super(getStatus(k).getStatusCode(), message, k);
    }

    public AccessDeniedException(int code, String message) {
        super(code, message, k);
    }
}
