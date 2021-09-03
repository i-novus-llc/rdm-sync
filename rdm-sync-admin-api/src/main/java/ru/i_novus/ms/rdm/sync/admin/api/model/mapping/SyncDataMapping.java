package ru.i_novus.ms.rdm.sync.admin.api.model.mapping;

import ru.i_novus.ms.rdm.sync.admin.api.model.book.SyncStructure;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

/**
 * Соответствие значений полей структуры (данных).
 */
@SuppressWarnings("unused")
public class SyncDataMapping {

    private SyncStructureMapping structureMapping;

    private SyncStructure oldStructure;

    private SyncStructure newStructure;

    public SyncDataMapping(SyncStructureMapping structureMapping,
                           SyncStructure oldStructure, SyncStructure newStructure) {
        this.structureMapping = structureMapping;
        this.oldStructure = oldStructure;
        this.newStructure = newStructure;
    }

    public SyncStructureMapping getStructureMapping() {
        return structureMapping;
    }

    public void setStructureMapping(SyncStructureMapping structureMapping) {
        this.structureMapping = structureMapping;
    }

    public SyncStructure getOldStructure() {
        return oldStructure;
    }

    public void setOldStructure(SyncStructure oldStructure) {
        this.oldStructure = oldStructure;
    }

    public SyncStructure getNewStructure() {
        return newStructure;
    }

    public void setNewStructure(SyncStructure newStructure) {
        this.newStructure = newStructure;
    }

    /**
     * Преобразование старых данных в новые.
     *
     * @param oldData старые данные
     * @return Новые данные
     */
    public List<Map<String, Object>> convert(Map<String, Object> oldData) {
        return singletonList(oldData);
    }
}
