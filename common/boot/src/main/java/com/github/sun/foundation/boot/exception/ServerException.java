package com.github.sun.foundation.boot.exception;

public class ServerException extends ResponsiveException {
    private static final long serialVersionUID = -7874845226963224487L;

    private static final Kind k = Kind.SERVER_ERROR;

    public ServerException(int code) {
        super(code, null, k);
    }

    public ServerException(String message) {
        super(getStatus(k).getStatusCode(), message, k);
    }

    public ServerException(int code, String message) {
        super(code, message, k);
    }

    public ServerException(String message, Throwable cause) {
        super(message, k, cause);
    }
}
