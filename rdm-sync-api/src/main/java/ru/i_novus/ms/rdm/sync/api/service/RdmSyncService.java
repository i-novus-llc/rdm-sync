package ru.i_novus.ms.rdm.sync.api.service;

import io.swagger.annotations.*;
import ru.i_novus.ms.rdm.sync.api.log.Log;
import ru.i_novus.ms.rdm.sync.api.log.LogCriteria;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * @author lgalimova
 * @since 20.02.2019
 */

@Path("/rdm")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Синхронизация данных справочников НСИ")
@SuppressWarnings("I-novus:MethodNameWordCountRule")
public interface RdmSyncService {

    @POST
    @Path("/update")
    @ApiOperation(value = "Синхронизация всех справочников")
    void update();

    @POST
    @Path("/update/{refBookCode}")
    @ApiOperation(value = "Синхронизация отдельного справочника")
    void update(@PathParam("refBookCode") String refBookCode);

    @GET
    @Path("/log")
    @ApiOperation(value = "Получение журнала за дату")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Успех"),
            @ApiResponse(code = 400, message = "Укажите пожалуйста дату в формате ISO_LOCAL_DATE [yyyy-MM-dd].")
    })
    List<Log> getLog(@BeanParam LogCriteria criteria);

    @GET
    @Path("/xml-fm")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response downloadXmlFieldMapping(@QueryParam("code") List<String> refBookCodes);


}
