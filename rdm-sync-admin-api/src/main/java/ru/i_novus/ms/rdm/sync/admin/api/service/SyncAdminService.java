package ru.i_novus.ms.rdm.sync.admin.api.service;

import io.swagger.annotations.*;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.sync.admin.api.model.source.*;
import ru.i_novus.ms.rdm.sync.admin.api.model.entry.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Сервис настройки синхронизации справочников.
 */
@Path("/sync/admin")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Сервис настройки синхронизации справочников")
public interface SyncAdminService {

    @GET
    @Path("/entries")
    @ApiOperation(value = "Поиск записей о синхронизации справочников")
    @ApiImplicitParams(@ApiImplicitParam(name = "sort", value = "Параметры сортировки",
            required = false, allowMultiple = true, paramType = "query", dataType = "string"))
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список записей о синхронизации"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    Page<SyncEntry> search(@BeanParam SyncEntryCriteria criteria);

    @GET
    @Path("/entries/{id}")
    @ApiOperation(value = "Получение записи о синхронизации справочника по идентификатору записи")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Запись о синхронизации"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    SyncEntry getById(@ApiParam("Идентификатор записи") @PathParam("id") String id);

    @GET
    @Path("/entries/code/{code}")
    @ApiOperation(value = "Получение записи о синхронизации справочника по коду справочника")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Запись о синхронизации"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    SyncEntry getByCode(@ApiParam("Код справочника") @PathParam("code") String code);

    @GET
    @Path("/entries/versions/")
    @ApiOperation(value = "Получение версий записи о синхронизации")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список версий записи"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    Page<SyncEntryVersion> searchVersions(@BeanParam SyncEntryVersionCriteria criteria);

    @GET
    @Path("/entries/mapping")
    @ApiOperation(value = "Получение маппинга версии записи о синхронизации")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Запись о синхронизации"),
            @ApiResponse(code = 404, message = "Нет ресурса")
    })
    SyncEntryMapping findMapping(@BeanParam SyncEntryMappingCriteria criteria);

    @GET
    @Path("/sources")
    @ApiOperation(value = "Поиск источников для синхронизации справочников")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список источников"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    Page<SyncEntrySource> searchSources(@BeanParam SyncEntrySourceCriteria criteria);

    @GET
    @Path("/sources/refbooks")
    @ApiOperation(value = "Поиск справочников в источнике")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список источников"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    Page<SyncSourceRefBook> searchSourceRefBooks(@BeanParam SyncSourceRefBookCriteria criteria);

    @GET
    @Path("/sources/refbooks/find")
    @ApiOperation(value = "Поиск справочника (с заданной версией) в источнике")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список источников"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    SyncSourceRefBook findSourceRefBook(@BeanParam SyncSourceRefBookCriteria criteria);

    @GET
    @Path("/sources/refbooks/validate/{sourceCode}/{code}")
    @ApiOperation(value = "Валидация справочника по источнику и коду")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список источников"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    String validateSourceRefBook(
            @ApiParam("Код источника") @PathParam("sourceCode") String sourceCode,
            @ApiParam("Код справочника") @PathParam("code") String code
    );

    @GET
    @Path("/sources/versions")
    @ApiOperation(value = "Поиск версий справочника в источнике")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Список источников"),
            @ApiResponse(code = 400, message = "Некорректный запрос")
    })
    Page<SyncSourceRefBook> searchSourceVersions(@BeanParam SyncSourceVersionCriteria criteria);
}
