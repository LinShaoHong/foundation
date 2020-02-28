package com.github.sun.foundation.boot.exception;

public class ConstraintException extends ResponsiveException {
    private static final long serialVersionUID = 3539017909338032930L;

    private static final Kind k = Kind.ANTI_CONSTRAINT;

    public ConstraintException(int code) {
        super(code, null, k);
    }

    public ConstraintException(String message) {
        super(getStatus(k).getStatusCode(), message, k);
    }

    public ConstraintException(int code, String message) {
        super(code, message, k);
    }

    public ConstraintException(String message, Throwable cause) {
        super(message, k, cause);
    }
}
