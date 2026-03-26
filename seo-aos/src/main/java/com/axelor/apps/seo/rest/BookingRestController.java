package com.axelor.apps.seo.rest;

import com.axelor.apps.seo.rest.dto.*;
import com.axelor.apps.seo.rest.dto.viewsDto.Views;
import com.axelor.apps.seo.service.*;
import com.fasterxml.jackson.annotation.JsonView;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("public/api/seo/booking")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingRestController {
    private final BookingService bookingService;

    @Inject
    public BookingRestController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @POST
    @Path("/create")
    @JsonView(Views.Create.class)
    public Response createBooking(BookingRequestDTO request) {
        return bookingService.create(request);
    }

    @PUT
    @Path("/update/{id}")
    @JsonView(Views.Update.class)
    public Response updateBooking(@PathParam("id") Long id, @QueryParam("codeWord") String codeWord,
                                  BookingRequestDTO request) {
        return bookingService.update(id, codeWord, request);
    }

    @DELETE
    @Path("/cancel/{id}")
    public Response cancelBooking(@PathParam("id") Long id, @QueryParam("codeWord") String codeWord) {
        return bookingService.cancel(id, codeWord);
    }

    @GET
    @Path("/slots")
    public Response getSlots(@QueryParam("date") String date, @QueryParam("ccpId") Long ccpId,
                             @QueryParam("status") String status) {
        return bookingService.getSlots(date, ccpId);
    }

    @GET
    @Path("/get-by")
    @JsonView(Views.Update.class)
    public Response getBookingsByDate(@QueryParam("date") String date, @QueryParam("ccpId") Long ccpId) {
        return bookingService.getByDate(date, ccpId);
    }

    @GET
    @Path("/by-plate-and-code")
    public Response getByPlateAndCode(@QueryParam("plateNo") String plateNo, @QueryParam("codeWord") String codeWord) {
        return bookingService.getByPlateAndCode(plateNo, codeWord);
    }

    @GET
    @Path("/active-reg-for-booking")
    public Response getActiveRegistrationForBooking(@QueryParam("plateNo") String plateNo, @QueryParam("ccpId") Long ccpId) {
        return bookingService.activeRegistrationForBooking(plateNo, ccpId);
    }
}