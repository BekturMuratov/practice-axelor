package com.axelor.apps.seo.rest;

import com.axelor.apps.seo.rest.dto.LetterDTO;
import com.axelor.apps.seo.service.LetterService;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("public/api/letter")
public class LetterRestController {

    private final LetterService letterService;

    @Inject
    public LetterRestController(LetterService letterService) {
        this.letterService = letterService;
    }

    @POST
    @Path("/send")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendLetter(LetterDTO letterDTO) {
        LetterDTO newLetter = letterService.sendLetter(letterDTO);
        return Response.status(Response.Status.CREATED).entity(newLetter).build();
    }
}
