package com.github.sun.foundation.boot.exception;

public class NotFoundException extends ResponsiveException {
    private static final long serialVersionUID = 3802162009940328816L;

    private static final Kind k = Kind.NOT_FOUND;

    public NotFoundException(int code) {
        super(code, null, k);
    }

    public NotFoundException(String message) {
        super(getStatus(k).getStatusCode(), message, k);
    }

    public NotFoundException(int code, String message) {
        super(code, message, k);
    }
}
