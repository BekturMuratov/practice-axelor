package com.axelor.apps.seo.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Path("public/api/seo/server")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServerTimeRestController {

    @GET
    @Path("/get-time")
    public Response getServerTime() {
        LocalDateTime now = LocalDateTime.now();
        String formattedDateTime = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return Response.ok(Map.of(
                "serverDateTime", formattedDateTime
        )).build();
    }
}