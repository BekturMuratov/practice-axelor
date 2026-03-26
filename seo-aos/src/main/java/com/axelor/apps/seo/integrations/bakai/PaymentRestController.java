package com.axelor.apps.seo.integrations.bakai;

import com.axelor.apps.seo.service.PaymentService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("public/api/seo/payment")
public class PaymentRestController {
    private final PaymentService paymentService;

    @Inject
    public PaymentRestController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @POST
    @Path("/generateQr")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generateQr(@QueryParam("requisiteNumber") String requisiteNumber) {
        return paymentService.generateQr(requisiteNumber);
    }

    @GET
    @Path("/qr-page")
    @Produces(MediaType.TEXT_HTML)
    public Response qrPage(@QueryParam("requisite") String requisiteNumber) {
        String html = paymentService.buildPaymentPageHtml(requisiteNumber);
        return Response.ok(html, MediaType.TEXT_HTML).build();
    }
}