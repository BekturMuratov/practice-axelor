package com.axelor.apps.seo.rest;

import com.axelor.apps.seo.rest.dto.DocumentCheckDTO;
import com.axelor.apps.seo.utils.ConnectionUtils;

import javax.ws.rs.*;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import java.util.*;

import static javax.ws.rs.core.Response.Status.*;

@Path("public/api/seo/gateway")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GatewayRestController {
    private static final String GATEWAY_PREFIX = "itg/ws/api/gateway/kezek/";

    @POST
    @Path("/check-document")
    public Response checkDocument(DocumentCheckDTO request) {
        if (request.getDeclarationType() == null || request.getDocumentNumber() == null) {
            return Response.status(BAD_REQUEST)
                    .entity(Map.of("error", "declarationType and documentNumber are required"))
                    .build();
        }
        try {
            String gatewayUrl = ConnectionUtils.getApiGatewayUrl() + GATEWAY_PREFIX + "check-document";
            Client client = ClientBuilder.newClient();
            Response gatewayResponse = client.target(gatewayUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION,
                            ConnectionUtils.getBasicAuthorization(
                                    ConnectionUtils.getApiGatewayUsername(),
                                    ConnectionUtils.getApiGatewayPassword()
                            ))
                    .post(Entity.json(request));
            String responseBody = gatewayResponse.readEntity(String.class);

            return Response.status(gatewayResponse.getStatus())
                    .entity(responseBody)
                    .build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
