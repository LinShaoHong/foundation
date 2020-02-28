package com.github.sun.foundation.boot.exception;

public class TimeoutException extends ResponsiveException {
    private static final long serialVersionUID = -8068520179505717411L;

    private static final Kind k = Kind.TIMEOUT;

    public TimeoutException(int code) {
        super(code, null, k);
    }

    public TimeoutException(String message) {
        super(getStatus(k).getStatusCode(), message, k);
    }

    public TimeoutException(int code, String message) {
        super(code, message, k);
    }

    public TimeoutException(String message, Throwable cause) {
        super(message, k, cause);
    }
}
