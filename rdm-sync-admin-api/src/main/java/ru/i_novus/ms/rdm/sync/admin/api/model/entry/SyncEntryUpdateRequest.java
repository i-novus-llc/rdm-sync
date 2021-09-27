package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;

import javax.ws.rs.QueryParam;
import java.io.Serializable;
import java.util.List;

@ApiModel(value = "Модель изменения записи о синхронизации",
        description = "Набор входных параметров для изменения записи о синхронизации")
@Getter
@Setter
public class SyncEntryUpdateRequest implements Serializable {

    @ApiParam("Идентификатор записи")
    @QueryParam("id")
    private String id;

    @ApiModelProperty("Код (идентификатор) источника")
    private String sourceCode;

    @ApiModelProperty("Код справочника")
    private String code;

    @ApiModelProperty("Наименование справочника")
    private String name;

    @ApiModelProperty("Версия (номер) справочника, которую планируется сделать текущей")
    private String version;

    @ApiModelProperty("Поддержка версионности")
    private Boolean versioned;

    @ApiModelProperty("Признак автообновления")
    private Boolean autoUpdatable;

    @ApiModelProperty("Код локального хранилища")
    private String storageCode;

    @ApiModelProperty("Маппинг полей версии")
    private List<SyncMappingField> mappingFields;
}
