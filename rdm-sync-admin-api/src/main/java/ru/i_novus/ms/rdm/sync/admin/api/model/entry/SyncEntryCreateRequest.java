package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@ApiModel(value = "Модель создания записи о синхронизации",
        description = "Набор входных параметров для создания записи о синхронизации")
@Getter
@Setter
public class SyncEntryCreateRequest implements Serializable {

    @ApiModelProperty("Код (идентификатор) источника")
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

    @ApiModelProperty("Код локального хранилища")
    private String storageCode;

    @ApiModelProperty("Маппинг полей версии")
    private List<SyncMappingField> mappingFields;
}
