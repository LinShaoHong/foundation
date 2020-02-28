package com.github.sun.foundation.boot.exception;

public class UnAuthorizedException extends ResponsiveException {
    private static final long serialVersionUID = -393855691095241950L;

    private static final Kind k = Kind.UNAUTHORIZED;

    public UnAuthorizedException(int code) {
        super(code, null, k);
    }

    public UnAuthorizedException(String message) {
        super(getStatus(k).getStatusCode(), message, k);
    }

    public UnAuthorizedException(int code, String message) {
        super(code, message, k);
    }
}
