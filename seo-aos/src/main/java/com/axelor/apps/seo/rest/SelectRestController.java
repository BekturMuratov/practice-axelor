package com.axelor.apps.seo.rest;

import com.axelor.apps.seo.service.SelectService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.axelor.apps.seo.utils.StatusConstants.*;

@Path("public/api/seo/select")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SelectRestController {
    private final SelectService selectService;

    @Inject
    public SelectRestController(SelectService selectService) {
        this.selectService = selectService;
    }

    @GET
    @Path("/queue-type")
    public Response getQueueType() {
        return Response.ok(selectService.getSelection(REGISTRATION_QUEUE_TYPE_SELECT)).build();
    }

    @GET
    @Path("/type-of-cargo")
    public Response getTypeOfCargo() {
        return Response.ok(selectService.getSelection(REGISTRATION_TYPE_OF_CARGO_SELECT)).build();
    }

    @GET
    @Path("/declaration-type")
    public Response getDeclarationType() {
        return Response.ok(selectService.getSelection(REGISTRATION_DECLARATION_TYPE_SELECT)).build();
    }

    @GET
    @Path("/all-slots")
    public Response getBookingSlots() {
        return Response.ok(selectService.getSelection(BOOKING_SLOTS_SELECT)).build();
    }

    @GET
    @Path("/statuses")
    public Response getBookingStatuses() {
        return Response.ok(selectService.getSelection(BOOKING_STATUS_SELECT)).build();
    }

    @GET
    @Path("/registration-statuses")
    public Response getRegistrationStatuses() {
        return Response.ok(selectService.getSelection(REGISTRATION_STATUS_SELECT)).build();
    }
}
