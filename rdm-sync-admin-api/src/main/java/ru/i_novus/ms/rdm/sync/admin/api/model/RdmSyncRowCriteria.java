package ru.i_novus.ms.rdm.sync.admin.api.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.i_novus.ms.rdm.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;

@Getter
@Setter
@ToString(callSuper = true)
@ApiModel("Критерий поиска записей о синхронизации справочников")
public class RdmSyncRowCriteria extends AbstractCriteria {

    @ApiModelProperty("Код справочника")
    @QueryParam("code")
    private String code;

    @ApiModelProperty("Наименование справочника")
    @QueryParam("name")
    private String name;
}
