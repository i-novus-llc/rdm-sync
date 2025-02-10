package ru.i_novus.ms.rdm.sync.api.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RefBookStructure {
    /**
     * Коды справочников на которые ссылается справочник
     */
    private List<String> references;

    /**
     * Коды первичных ключей
     */
    private List<String> primaries;

    private Set<Attribute> attributes;

    private String refDescription;

    public RefBookStructure() {
    }

    public RefBookStructure(List<String> references, List<String> primaries, Set<Attribute> attributes) {
        this.references = references == null ? Collections.emptyList() : references;
        this.primaries = primaries;
        this.attributes = attributes;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasPrimary() {
        return primaries != null && !primaries.isEmpty();
    }

    public Set<Attribute> getAttributes() {
        return attributes;
    }

    public Attribute getAttribute(String code) {
        if (attributes == null) {
            return null;
        }
        return attributes.stream().filter(attribute -> attribute.code().equals(code)).findAny().orElse(null);
    }

    public void setAttributes(Set<Attribute> attributes) {
        this.attributes = attributes;
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

    public String getRefDescription() {
        return refDescription;
    }

    public void setRefDescription(String refDescription) {
        this.refDescription = refDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        RefBookStructure that = (RefBookStructure) o;

        return
                new EqualsBuilder()
                        .append(references, that.references)
                        .append(primaries, that.primaries)
                        .append(attributes, that.attributes)
                        .append(refDescription, that.refDescription)
                        .isEquals();
    }

    @Override
    public int hashCode() {
        return
                new HashCodeBuilder(17, 37)
                        .append(references)
                        .append(primaries)
                        .append(attributes)
                        .append(refDescription)
                        .toHashCode();
    }

    @Override
    public String toString() {
        return "RefBookStructure{" +
                "references=" + references +
                ", primaries=" + primaries +
                ", attributes=" + attributes +
                ", refDescription=" + refDescription +
                '}';
    }

    public record Attribute(String code, AttributeTypeEnum type, String description) {
    }
}
