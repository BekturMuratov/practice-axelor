package com.axelor.apps.svh.web;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.repo.RegistrationRepository;
import com.axelor.apps.svh.service.ServicesService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;

import java.math.BigDecimal;

public class ServiceController {

    @Inject
    private ServicesService serviceCalculationService;

    public void calculationService(ActionRequest request,
                                   ActionResponse response) {

        // Берём объект прямо из контекста UI
        Registration registration = request.getContext().asType(Registration.class);

        if (registration == null || registration.getServices() == null) {
            response.setNotify("Нет услуг для расчёта");
            return;
        }

        BigDecimal servicesAmount =
                serviceCalculationService.calculationServiceAmount(registration);

        response.setValue("services_amount", servicesAmount);
    }
}

