package ru.i_novus.ms.rdm.sync.api.service;

import org.springframework.data.domain.Page;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

@Path("/rdm/data")
@Produces(MediaType.APPLICATION_JSON)
public interface LocalRdmDataService {

    @GET
    @Path("/{refBookCode}")
    @SuppressWarnings("squid:S1452")
    Page<Map<String, Object>> getData(
            @PathParam("refBookCode") String refBookCode,
            @QueryParam("getDeleted") Boolean getDeleted,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @Context UriInfo uriInfo
    );

    @GET
    @Path("/{refBookCode}/version/{version}")
    @SuppressWarnings("squid:S1452")
    Page<Map<String, Object>> getVersionedData(
            @PathParam("refBookCode") String refBookCode,
            @PathParam("version") String version,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @Context UriInfo uriInfo
    );

    @GET
    @Path("/{refBookCode}/{primaryKey}")
    Map<String, Object> getSingle(
            @PathParam("refBookCode") String refBookCode,
            @PathParam("primaryKey") String primaryKey
    );

    @GET
    @Path("/{refBookCode}/record/{recordId}")
    Map<String, Object> getBySystemId(
            @PathParam("refBookCode") String refBookCode,
            @PathParam("recordId") Long recordId
    );
}
