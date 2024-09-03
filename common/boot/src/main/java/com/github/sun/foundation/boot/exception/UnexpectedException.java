package com.github.sun.foundation.boot.exception;


import com.github.sun.foundation.boot.utility.Throws;

public class UnexpectedException extends ResponsiveException {
    private static final long serialVersionUID = -6792100941008920096L;

    private static final Kind k = Kind.UNEXPECTED;

    public UnexpectedException(int code) {
        super(code, null, k);
    }

    public UnexpectedException(String message) {
        super(getStatus(k).getStatusCode(), message, k);
    }

    public UnexpectedException(int code, String message) {
        super(code, message, k);
    }

    public UnexpectedException(String message, Throwable cause) {
        super(message, k, cause);
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        Throwable cause = getCause();
        if (cause != null) {
            message = message + "\nCause by " + Throws.stackTraceOf(cause);
        }
        return message;
    }
}
