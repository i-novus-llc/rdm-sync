package ru.i_novus.ms.rdm.sync.api.exception;

public class NoUniqueDefaultMappingException extends RdmSyncException {

    private static final String MESSAGE = "The default mapping, the one with range=null, should be unique. See refBook %s";

    public NoUniqueDefaultMappingException(String refBookCode) {
        super(String.format(MESSAGE, refBookCode));
    }

    public NoUniqueDefaultMappingException(Throwable cause, String refBookCode) {
        super(String.format(MESSAGE, refBookCode), cause);
    }

    public NoUniqueDefaultMappingException(Throwable cause, boolean enableSuppression, boolean writableStackTrace, String refBookCode) {
        super(String.format(MESSAGE, refBookCode), cause, enableSuppression, writableStackTrace);
    }
}
