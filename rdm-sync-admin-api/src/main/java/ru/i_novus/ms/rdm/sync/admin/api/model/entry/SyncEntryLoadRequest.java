package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;

import javax.ws.rs.QueryParam;
import java.io.Serializable;
import java.util.List;

@ApiModel(value = "Модель загрузки версии записи о синхронизации",
        description = "Набор входных параметров для загрузки версии записи о синхронизации")
@Getter
@Setter
public class SyncEntryLoadRequest implements Serializable {

    @ApiParam("Идентификатор записи")
    @QueryParam("entryId")
    private String entryId;

    @ApiModelProperty("Загружаемая версия (номер) справочника")
    private String startVersion;
}
