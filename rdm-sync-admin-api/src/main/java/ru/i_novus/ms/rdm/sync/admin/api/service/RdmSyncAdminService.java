package ru.i_novus.ms.rdm.sync.admin.api.service;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.admin.api.model.AbstractCriteria;
import ru.i_novus.ms.rdm.sync.admin.api.model.RdmSyncSource;
import ru.i_novus.ms.rdm.sync.admin.api.model.row.RdmSyncRow;
import ru.i_novus.ms.rdm.sync.admin.api.model.row.RdmSyncRowCriteria;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Сервис настройки синхронизации справочников.
 */
@Path("/sync/admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Сервис настройки синхронизации справочников")
public interface RdmSyncAdminService {

    @GET
    @Path("/rows")
    @ApiOperation(value = "Поиск записей о синхронизации справочников")
    @ApiImplicitParams(@ApiImplicitParam(name = "sort", value = "Параметры сортировки",
            required = false, allowMultiple = true, paramType = "query", dataType = "string"))
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список записей о синхронизации"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    Page<RdmSyncRow> search(@BeanParam RdmSyncRowCriteria criteria);

    @GET
    @Path("/rows/{id}")
    @ApiOperation(value = "Получение записи о синхронизации справочника по идентификатору записи")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Запись о синхронизации"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RdmSyncRow getById(@ApiParam("Идентификатор записи") @PathParam("id") String id);

    @GET
    @Path("/rows/code/{code}")
    @ApiOperation(value = "Получение записи о синхронизации справочника по коду справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Запись о синхронизации"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    RdmSyncRow getByCode(@ApiParam("Код справочника") @PathParam("code") String code);

    @GET
    @Path("/sources")
    @ApiOperation(value = "Поиск источников для синхронизации справочников")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список источников"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    Page<RdmSyncSource> searchSources(@BeanParam AbstractCriteria criteria);
}
