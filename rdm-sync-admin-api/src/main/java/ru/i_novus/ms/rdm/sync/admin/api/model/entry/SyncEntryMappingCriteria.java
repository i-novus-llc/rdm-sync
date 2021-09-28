package ru.i_novus.ms.rdm.sync.admin.api.model.entry;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;

import javax.ws.rs.QueryParam;
import java.util.Objects;

/**
 * Критерий поиска маппинга для версии записи.
 */
@Getter
@Setter
public class SyncEntryMappingCriteria extends AbstractCriteria {

    @ApiParam("Идентификатор записи")
    @QueryParam("entryId")
    private String entryId;

    @ApiParam("Идентификатор версии записи")
    @QueryParam("versionId")
    private String versionId;

    @ApiParam("Код (идентификатор) источника")
    @QueryParam("sourceCode")
    private String sourceCode;

    @ApiParam("Код справочника")
    @QueryParam("code")
    private String code;

    @ApiParam("Версия (номер) справочника")
    @QueryParam("version")
    private String version;

    @ApiParam("Поддержка версионности")
    @QueryParam("versioned")
    private Boolean versioned;

    @ApiParam("Признак автообновления")
    @QueryParam("autoUpdatable")
    private Boolean autoUpdatable;

    @ApiParam("Тип выполняемого действия")
    @QueryParam("actionType")
    private String actionType;

    @ApiParam("Текст для поиска по нескольким полям")
    @QueryParam("text")
    private String text;

    @ApiParam("Общее количество записей")
    @QueryParam("count")
    private Integer count;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SyncEntryMappingCriteria that = (SyncEntryMappingCriteria) o;
        return Objects.equals(entryId, that.entryId) &&
                Objects.equals(versionId, that.versionId) &&

                Objects.equals(sourceCode, that.sourceCode) &&
                Objects.equals(code, that.code) &&

                Objects.equals(version, that.version) &&
                Objects.equals(versioned, that.versioned) &&
                Objects.equals(autoUpdatable, that.autoUpdatable) &&

                Objects.equals(actionType, that.actionType) &&
                Objects.equals(text, that.text) &&
                Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), entryId, versionId, sourceCode, code,
                version, versioned, autoUpdatable, actionType, text, count);
    }
}
