package com.axelor.apps.seo.rest.exception;

import com.axelor.apps.seo.rest.exception.responce.ApiResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof BusinessException) {
            BusinessException ex = (BusinessException) exception;
            return Response.status(ex.getStatus())
                    .entity(ApiResponse.error(ex.getMessage()))
                    .build();
        }
        if (exception instanceof WebApplicationException) {
            WebApplicationException ex = (WebApplicationException) exception;
            int status = ex.getResponse().getStatus();
            String message = ex.getMessage() != null
                    ? ex.getMessage() : "Request error";

            return Response.status(status)
                    .entity(ApiResponse.error(message))
                    .build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.error("Internal server error"))
                .build();
    }
}
