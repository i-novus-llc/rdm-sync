package ru.i_novus.ms.rdm.sync.api.exception;

/**
 * Created by tnurdinov on 28.06.2018.
 */
public class RdmSyncException extends RuntimeException {

    public RdmSyncException() {
    }

    public RdmSyncException(String message) {
        super(message);
    }

    public RdmSyncException(String message, Throwable cause) {
        super(message, cause);
    }

    public RdmSyncException(Throwable cause) {
        super(cause);
    }

    public RdmSyncException(String message, Throwable cause,
                            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
