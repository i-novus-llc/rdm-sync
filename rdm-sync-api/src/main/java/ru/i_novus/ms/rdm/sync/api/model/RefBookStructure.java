package ru.i_novus.ms.rdm.sync.api.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
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
        this.references = references == null ? Collections.emptyList() : references;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        RefBookStructure that = (RefBookStructure) o;

        return new EqualsBuilder().append(references, that.references).append(primaries, that.primaries).append(attributesAndTypes, that.attributesAndTypes).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(references).append(primaries).append(attributesAndTypes).toHashCode();
    }

    @Override
    public String toString() {
        return "RefBookStructure{" +
                "references=" + references +
                ", primaries=" + primaries +
                ", attributesAndTypes=" + attributesAndTypes +
                '}';
    }
}
