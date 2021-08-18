package ru.i_novus.ms.rdm.sync.admin.api.model;

import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;

/**
 * Критерий поиска записей о синхронизации справочников.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class RdmSyncRowCriteria extends AbstractCriteria {

    @ApiParam("Код справочника")
    @QueryParam("code")
    private String code;

    @ApiParam("Наименование справочника")
    @QueryParam("name")
    private String name;
}
