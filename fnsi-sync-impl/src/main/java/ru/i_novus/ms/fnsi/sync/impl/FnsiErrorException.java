package ru.i_novus.ms.fnsi.sync.impl;

public class FnsiErrorException extends RuntimeException {

    private String message;

    public FnsiErrorException(String message) {
        this.message = message;
    }

    public FnsiErrorException(String message, String message1) {
        super(message);
        this.message = message1;
    }

    public FnsiErrorException(String message, Throwable cause, String message1) {
        super(message, cause);
        this.message = message1;
    }

    public FnsiErrorException(Throwable cause, String message) {
        super(cause);
        this.message = message;
    }

    public FnsiErrorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, String message1) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.message = message1;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
