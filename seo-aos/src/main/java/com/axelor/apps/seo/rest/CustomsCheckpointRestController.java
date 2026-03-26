package com.axelor.apps.seo.rest;

import com.axelor.apps.seo.db.repo.CustomsCheckpointRepository;
import com.axelor.apps.seo.rest.dto.CustomsCheckpointDTO;
import com.axelor.apps.seo.rest.mapper.CustomsCheckpointMapper;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;
import java.util.stream.Collectors;

@Path("public/api/seo/ccp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomsCheckpointRestController {

    private final CustomsCheckpointRepository  ccpRepository;

    @Inject
    public CustomsCheckpointRestController(CustomsCheckpointRepository ccpRepository) {
        this.ccpRepository = ccpRepository;
    }

    @GET
    @Path("/getCcp")
    public Response getCcp() {
        List<CustomsCheckpointDTO> ccpList = ccpRepository.all()
                .order("ccpName").fetch().stream()
                .map(CustomsCheckpointMapper::toDTO)
                .collect(Collectors.toList());
        return Response.ok(ccpList).build();
    }

    @GET
    @Path("/getOnlinePaymentCcp")
    public Response getOnlinePaymentCcp() {
        List<CustomsCheckpointDTO> ccpList = ccpRepository.all()
                .filter("self.onlinePayments = :onlinePayments")
                .bind("onlinePayments", "yes")
                .order("ccpName")
                .fetch()
                .stream()
                .map(CustomsCheckpointMapper::toDTO)
                .collect(Collectors.toList());
        return Response.ok(ccpList).build();
    }
}
