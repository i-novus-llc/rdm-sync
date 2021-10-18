package ru.i_novus.ms.rdm.sync.api.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Map;

public class RowDiff {

    private final RowDiffStatusEnum status;

    private final Map<String, Object> row;

    public RowDiff(RowDiffStatusEnum status, Map<String, Object> row) {
        this.status = status;
        this.row = row;
    }

    public RowDiffStatusEnum getStatus() {
        return status;
    }

    public Map<String, Object> getRow() {
        return row;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        RowDiff rowDiff = (RowDiff) o;

        return new EqualsBuilder().append(status, rowDiff.status).append(row, rowDiff.row).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(status).append(row).toHashCode();
    }

    @Override
    public String toString() {
        return "RowDiff{" +
                "status=" + status +
                ", row=" + row +
                '}';
    }
}
