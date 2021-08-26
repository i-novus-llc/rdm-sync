package ru.i_novus.ms.rdm.sync;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LongRowValue.class, name = "LongRowValue"),
        @JsonSubTypes.Type(value = RefBookRowValue.class, name = "RefBookRowValue"),
})
public class TestRowValueMixin {
}
