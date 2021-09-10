package ru.i_novus.ms.rdm.sync.admin.n2o.constant;

import java.util.Arrays;
import java.util.List;

public class SyncMappingConstants {

    public static final String ACTION_TYPE_CREATE = "create";
    public static final String ACTION_TYPE_UPDATE = "update";

    private static final List<String> ACTION_TYPES = Arrays.asList(ACTION_TYPE_CREATE, ACTION_TYPE_UPDATE);

    public static final String FIELD_SOURCE_CODE = "sourceCode";
    public static final String FIELD_REFBOOK_CODE = "code";
    public static final String FIELD_REFBOOK_NAME = "name";
    public static final String FIELD_VERSION_VERSION = "version";
    public static final String FIELD_VERSIONED = "versioned";
    public static final String FIELD_AUTO_UPDATABLE = "autoUpdatable";
    public static final String FIELD_ACTION_TYPE = "actionType";
}
