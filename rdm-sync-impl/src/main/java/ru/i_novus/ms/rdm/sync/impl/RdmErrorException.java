package ru.i_novus.ms.rdm.sync.impl;

public class RdmErrorException extends RuntimeException {

    public RdmErrorException(String message) {
        super(message);
    }

    public RdmErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
