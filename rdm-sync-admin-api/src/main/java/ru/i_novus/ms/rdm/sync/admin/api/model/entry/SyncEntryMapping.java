package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.utils.JsonUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Модель маппинга для версии записи.
 * <p/>
 * Используется как при создании записи о синхронизации, так и при её изменении.
 */
@Getter
@Setter
public class SyncEntryMapping implements Serializable {

    //public static final String KEY_INDEX_NAME_SEPARATOR = ".";
    //public static final String KEY_NAME_OLD_FIELD_CODE = "old";
    //public static final String KEY_NAME_NEW_FIELD_CODE = "new";
    //public static final String KEY_NAME_NEW_FIELD_NAME = "name";
    //public static final String KEY_NAME_NEW_FIELD_TYPE = "type";

    /** Запись о синхронизации. */
    private SyncEntry entry;

    /** Версия записи о синхронизации. */
    private SyncEntryVersion version;

    ///** Идентификатор записи. */
    //private String entryId;
    //
    ///** Идентификатор версии записи. */
    //private String versionId;
    //
    ///** Код (идентификатор) источника. */
    //private String sourceCode;
    //
    ///** Код справочника. */
    //private String code;
    //
    ///** Версия (номер) справочника. */
    //private String version;
    //
    ///** Поддержка версионности. */
    //private boolean versioned = false;
    //
    ///** Признак автообновления. */
    //private boolean autoUpdatable = false;

    //private Map<String, Serializable> fieldMappings;
    private List<SyncMappingField> fieldMappings;

    public SyncEntryMapping() {
        // Nothing to do.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncEntryMapping that = (SyncEntryMapping) o;
        return //Objects.equals(entryId, that.entryId) &&
                //Objects.equals(versionId, that.versionId) &&
                //
                //Objects.equals(sourceCode, that.sourceCode) &&
                //Objects.equals(code, that.code) &&
                //Objects.equals(version, that.version) &&
                //
                //Objects.equals(versioned, that.versioned) &&
                //Objects.equals(autoUpdatable, that.autoUpdatable) &&

                Objects.equals(entry, that.entry) &&
                Objects.equals(version, that.version) &&
                Objects.equals(fieldMappings, that.fieldMappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(//entryId, versionId,
                //sourceCode, code, version,
                //versioned, autoUpdatable,
                entry, version,
                fieldMappings);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
