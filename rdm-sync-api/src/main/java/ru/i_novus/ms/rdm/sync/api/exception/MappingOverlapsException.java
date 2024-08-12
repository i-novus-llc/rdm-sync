package ru.i_novus.ms.rdm.sync.api.exception;

public class MappingOverlapsException extends RuntimeException {

    private static final String MESSAGE = "Overlapping version ranges: %s and %s detected for refBookCode: %s";

    private final String overlappingRange1;

    private final String overlappingRange2;

    private final String refBookCode;

    public MappingOverlapsException(String overlappingRange1, String overlappingRange2, String refBookCode) {
        super(String.format(MESSAGE, overlappingRange1, overlappingRange2, refBookCode));
        this.overlappingRange1 = overlappingRange1;
        this.overlappingRange2 = overlappingRange2;
        this.refBookCode = refBookCode;
    }

    public MappingOverlapsException(Throwable cause, String overlappingRange1, String overlappingRange2, String refBookCode) {
        super(String.format(MESSAGE, overlappingRange1, overlappingRange2, refBookCode), cause);
        this.overlappingRange1 = overlappingRange1;
        this.overlappingRange2 = overlappingRange2;
        this.refBookCode = refBookCode;
    }


    public MappingOverlapsException(Throwable cause, boolean enableSuppression, boolean writableStackTrace, String overlappingRange1, String overlappingRange2, String refBookCode) {
        super(String.format(MESSAGE, overlappingRange1, overlappingRange2, refBookCode), cause, enableSuppression, writableStackTrace);
        this.overlappingRange1 = overlappingRange1;
        this.overlappingRange2 = overlappingRange2;
        this.refBookCode = refBookCode;
    }

    public String getOverlappingRange1() {
        return overlappingRange1;
    }

    public String getOverlappingRange2() {
        return overlappingRange2;
    }

    public String getRefBookCode() {
        return refBookCode;
    }
}
