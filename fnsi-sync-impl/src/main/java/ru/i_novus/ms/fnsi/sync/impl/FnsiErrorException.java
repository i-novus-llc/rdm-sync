package ru.i_novus.ms.fnsi.sync.impl;

public class FnsiErrorException extends RuntimeException {

    public FnsiErrorException(String message) {
        super(message);
    }

    public FnsiErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
