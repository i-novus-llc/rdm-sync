package ru.i_novus.ms.rdm.sync.admin.api.model.mapping;

import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.isEmpty;

/**
 * Соответствие полей структуры.
 */
public class SyncStructureMapping {

    /** Тип соответствия полей структуры. */
    private final SyncMappingTypeEnum type;

    /** Поля (версии) справочника при маппинге. */
    private List<SyncToFieldMapping> fieldMappings;

    private SyncStructureMapping(SyncMappingTypeEnum type) {
        this.type = type;
    }

    public SyncStructureMapping() {
        this(SyncMappingTypeEnum.NONE);
    }

    public SyncStructureMapping(List<SyncToFieldMapping> fieldMappings) {

        this(SyncMappingTypeEnum.LIST);

        this.fieldMappings = fieldMappings;
    }

    public SyncMappingTypeEnum getType() {
        return type;
    }

    public List<SyncToFieldMapping> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(List<SyncToFieldMapping> fieldMappings) {
        
        if (!isNone()) {
            this.fieldMappings = fieldMappings;
        }
    }

    public boolean isNone() {
        return SyncMappingTypeEnum.NONE.equals(type);
    }

    /**
     * Получение списка имён старых полей из соответствия полей.
     *
     * @return Список имён старых полей
     */
    public List<String> getOldFieldNames() {

        if (isNone() || isEmpty(fieldMappings))
            return Collections.emptyList();

        return fieldMappings.stream()
                .filter(fieldMapping -> fieldMapping instanceof FieldToFieldMapping)
                .map(fieldMapping -> ((FieldToFieldMapping) fieldMapping).getOldField())
                .filter(Objects::nonNull)
                .collect(toList());
    }

    /**
     * Получение имени нового поля из соответствия старому полю.
     *
     * @param oldFieldName имя старого поля
     * @return Имя нового поля
     */
    public String getNewFieldName(String oldFieldName) {

        if (isNone())
            return oldFieldName;

        if (isEmpty(oldFieldName) || CollectionUtils.isEmpty(fieldMappings))
            return null;

        return fieldMappings.stream()
                .filter(fieldMapping -> {
                    if (!(fieldMapping instanceof FieldToFieldMapping))
                        return false;

                    String name = ((FieldToFieldMapping) fieldMapping).getOldField();
                    return !isEmpty(name) && oldFieldName.equals(name);
                })
                .map(SyncToFieldMapping::getNewField)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    /**
     * Получение соответствия поля по имени нового поля.
     *
     * @param newFieldName имя нового поля
     * @return Соответствие поля
     */
    public SyncToFieldMapping getNewFieldMapping(String newFieldName) {

        if (isNone() || isEmpty(newFieldName) || CollectionUtils.isEmpty(fieldMappings))
            return null;

        return fieldMappings.stream()
                .filter(fieldMapping -> newFieldName.equals(fieldMapping.getNewField()))
                .findFirst().orElse(null);
    }

    public static SyncStructureMapping getNoneStructureMapping () {

        return new SyncStructureMapping(SyncMappingTypeEnum.NONE);
    }
}
