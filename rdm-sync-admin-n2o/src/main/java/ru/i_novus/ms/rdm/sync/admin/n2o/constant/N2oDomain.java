package ru.i_novus.ms.rdm.sync.admin.n2o.constant;

import org.springframework.util.StringUtils;

/**
 * Типы данных N2O в соответствии с типами полей.
 *
 * @see net.n2oapp.framework.api.metadata.domain.Domain
 */
public class N2oDomain {

    public static final String STRING = "string";
    public static final String INTEGER = "integer";
    public static final String LONG = "long";
    public static final String FLOAT = "numeric";
    public static final String LOCALDATE = "localdate";
    public static final String LOCALDATETIME = "localdatetime";
    public static final String BOOLEAN = "boolean";

    private N2oDomain() {
        // Nothing to do.
    }

    public static String fieldTypeToDomain(String fieldType) {

        if (StringUtils.isEmpty(fieldType))
            return N2oDomain.STRING;

        return switch (fieldType) {
            case "STRING" -> N2oDomain.STRING;
            case "INTEGER" -> N2oDomain.INTEGER;
            case "FLOAT" -> N2oDomain.FLOAT;
            case "DATE" -> N2oDomain.LOCALDATE;
            case "DATETIME" -> N2oDomain.LOCALDATETIME;
            case "BOOLEAN" -> N2oDomain.BOOLEAN;
            default -> throw new IllegalArgumentException(String.format("Field type '%s' is not supported", fieldType));
        };
    }
}
