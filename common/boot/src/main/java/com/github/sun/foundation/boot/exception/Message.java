package com.github.sun.foundation.boot.exception;

public class Message extends ResponsiveException {
    private static final long serialVersionUID = -4251755347922068419L;

    private static final Kind k = Kind.MESSAGE;

    public Message(int code) {
        super(code, null, k);
    }
}
