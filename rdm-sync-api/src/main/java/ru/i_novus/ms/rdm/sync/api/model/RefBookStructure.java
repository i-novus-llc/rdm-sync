package ru.i_novus.ms.rdm.sync.api.model;

import java.util.List;
import java.util.Map;

public class RefBookStructure {
    /**
     * Коды справочников на которые ссылается справочник
     */
    private List<String> references;

    /**
     * Коды первичных ключей
     */
    private List<String> primaries;

    /**
     * Ключ код атрибута, значение его тип
     */
    private Map<String, AttributeTypeEnum> attributesAndTypes;

    public RefBookStructure() {
    }

    public RefBookStructure(List<String> references, List<String> primaries, Map<String, AttributeTypeEnum> attributesAndTypes) {
        this.references = references;
        this.primaries = primaries;
        this.attributesAndTypes = attributesAndTypes;
    }

    public boolean hasPrimary() {
        return primaries != null && !primaries.isEmpty();
    }

    public Map<String, AttributeTypeEnum> getAttributesAndTypes() {
        return attributesAndTypes;
    }

    public void setAttributesAndTypes(Map<String, AttributeTypeEnum> attributesAndTypes) {
        this.attributesAndTypes = attributesAndTypes;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    public List<String> getPrimaries() {
        return primaries;
    }

    public void setPrimaries(List<String> primaries) {
        this.primaries = primaries;
    }
}
