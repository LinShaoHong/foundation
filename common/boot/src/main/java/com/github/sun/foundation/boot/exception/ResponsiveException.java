package com.github.sun.foundation.boot.exception;


import javax.ws.rs.core.Response;

public abstract class ResponsiveException extends RuntimeException {
    private static final long serialVersionUID = -7775372746962627516L;

    /**
     * The kind of responsive exception
     */
    public enum Kind {
        /**
         * Throw a message to notify something, it's not a error
         */
        MESSAGE,
        /**
         * When parameters are wrong
         */
        BAD_REQUEST,
        /**
         * When can not find the resource
         */
        NOT_FOUND,
        /**
         * When the business constraint are broken
         */
        ANTI_CONSTRAINT,
        /**
         * When there are no permissions
         */
        ACCESS_DENIED,
        /**
         * When is not authorized, like did not login
         */
        UNAUTHORIZED,
        /**
         * Time out
         */
        TIMEOUT,
        /**
         * Server error
         */
        SERVER_ERROR,
        /**
         * When some unexpected exception occur
         */
        UNEXPECTED
    }

    private int code;
    private Kind kind;

    public ResponsiveException(int code, String message, Kind kind) {
        super(message);
        this.kind = kind;
        this.code = code;
    }

    public ResponsiveException(String message, Kind kind, Throwable cause) {
        super(message, cause);
        this.code = getStatus(kind).getStatusCode();
        this.kind = kind;
    }

    public int getCode() {
        return code;
    }

    public Kind getKind() {
        return kind;
    }

    static Response.Status getStatus(Kind kind) {
        switch (kind) {
            case MESSAGE:
                return Response.Status.OK;
            case ACCESS_DENIED:
                return Response.Status.FORBIDDEN;
            case BAD_REQUEST:
                return Response.Status.BAD_REQUEST;
            case UNAUTHORIZED:
                return Response.Status.UNAUTHORIZED;
            case NOT_FOUND:
                return Response.Status.NOT_FOUND;
            case TIMEOUT:
                return Response.Status.REQUEST_TIMEOUT;
            case SERVER_ERROR:
                return Response.Status.SERVICE_UNAVAILABLE;
            case UNEXPECTED:
                return Response.Status.INTERNAL_SERVER_ERROR;
            case ANTI_CONSTRAINT:
                return Response.Status.PRECONDITION_FAILED;
            default:
                throw new IllegalStateException("Illegal exception kind with " + kind.name());
        }
    }
}
