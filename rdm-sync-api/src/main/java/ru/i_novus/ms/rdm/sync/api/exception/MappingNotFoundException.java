package ru.i_novus.ms.rdm.sync.api.exception;

public class MappingNotFoundException extends RuntimeException {


    public MappingNotFoundException(String refCode, String refVersion) {
        super("No mapping for refbook " + refCode + " ver " + refVersion);
    }
}
