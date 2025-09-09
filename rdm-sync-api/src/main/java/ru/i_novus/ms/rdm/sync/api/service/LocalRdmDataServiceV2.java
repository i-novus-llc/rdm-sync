package ru.i_novus.ms.rdm.sync.api.service;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.springframework.data.domain.Page;

import java.util.Map;

@Path("/rdm/data/v2")
@Produces(MediaType.APPLICATION_JSON)
public interface LocalRdmDataServiceV2 {

    @GET
    @Path("/{refBookCode}")
    @SuppressWarnings("squid:S1452")
    Page<Map<String, Object>> getData(
            @PathParam("refBookCode") String refBookCode,
            @QueryParam("getDeleted") Boolean getDeleted,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @QueryParam("filter") String rsqlFilter,
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
            @QueryParam("filter") String rsqlFilter,
            @Context UriInfo uriInfo
    );
}
