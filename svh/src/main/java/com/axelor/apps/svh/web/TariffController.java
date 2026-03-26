package com.axelor.apps.svh.web;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.service.TariffService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class TariffController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TariffController.class);

    private final TariffService tariffService;

    @Inject
    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    public void calculateTariff(ActionRequest request, ActionResponse response) {

        try {

            Registration registration = request.getContext().asType(Registration.class);

            BigDecimal result = tariffService.calculateTariffByRegistration(registration.getId());

            int storageDays = tariffService.calculateStorageDays(
                    registration.getCreatedOn(),
                    registration.getReleasedOn()
            );

            response.setValue("storage_days", storageDays);
            response.setValue("calculated_amount", result);
            LOGGER.info("createdOn: {}", registration.getCreatedOn());
            LOGGER.info("calculated_amount: {}", result);


        } catch (Exception e) {

            LOGGER.error("Error while calculation tariff", e);

        }
    }

}