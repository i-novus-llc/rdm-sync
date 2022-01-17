package ru.i_novus.ms.rdm.sync.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.time.LocalDateTime;

public class RefBookPassport {

    private final String version;

    private final LocalDateTime from;

    private final LocalDateTime to;

    public RefBookPassport(String version, LocalDateTime from, LocalDateTime to) {
        this.version = version;
        this.from = from;
        this.to = to;
    }

    public String getVersion() {
        return version;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        RefBookPassport that = (RefBookPassport) o;

        return new EqualsBuilder().append(version, that.version).append(from, that.from).append(to, that.to).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(version).append(from).append(to).toHashCode();
    }

    @Override
    public String toString() {
        return "RefBookPassport{" +
                "version='" + version + '\'' +
                ", from=" + from +
                ", to=" + to +
                '}';
    }
}
