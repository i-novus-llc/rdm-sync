package ru.i_novus.ms.rdm.sync.admin.api.model.row;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@ApiModel(value = "Модель создания записи о синхронизации",
        description = "Набор входных параметров для создания записи о синхронизации")
@Getter
@Setter
public class RdmSyncRowCreateRequest implements Serializable {

    @ApiModelProperty("Источник для синхронизации")
    private String sourceCode;

    @ApiModelProperty("Код справочника")
    private String code;

    @ApiModelProperty("Наименование справочника")
    private String name;

    @ApiModelProperty("Версия (номер) справочника, с которой планируется загрузка справочника")
    private String startVersion;

    @ApiModelProperty("Поддержка версионности")
    private Boolean versioned;

    @ApiModelProperty("Признак автообновления")
    private Boolean autoUpdatable;
}
