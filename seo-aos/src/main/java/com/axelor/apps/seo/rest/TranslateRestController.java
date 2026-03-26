package com.axelor.apps.seo.rest;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;

import static javax.ws.rs.core.Response.Status.*;

@Path("public/api/seo/meta")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TranslateRestController {

    private final EntityManager entityManager;

    @Inject
    public TranslateRestController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GET
    @Path("/translations")
    public Response getTranslations() {
        try {
            List<Object[]> rows = entityManager.createQuery(
                    "SELECT t.language, t.key, t.message FROM MetaTranslation t",
                    Object[].class
            ).getResultList();

            Map<String, Map<String, String>> translations = new HashMap<>();
            for (Object[] row : rows) {
                String language = (String) row[0];
                String key = (String) row[1];
                String value = (String) row[2];

                translations.computeIfAbsent(language, k -> new HashMap<>())
                        .put(key, value);
            }
            return Response.ok(translations).build();

        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(Map.of("success", false, "error", e.getMessage()))
                    .build();
        }
    }
}
