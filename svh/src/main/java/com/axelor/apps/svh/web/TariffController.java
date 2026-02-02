package com.axelor.apps.svh.web;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.repo.RegistrationRepository;
import com.axelor.apps.svh.service.TariffService;
import com.google.inject.Inject;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

import javax.transaction.Transactional;
import java.math.BigDecimal;

public class TariffController {

    @Inject
    private TariffService tariffService;

    @Transactional
    public void calculateTariff(ActionRequest request, ActionResponse response) {

        Registration registration = request.getContext().asType(Registration.class);

        if (registration == null || registration.getId() == null) {
            response.setError("Сначала сохраните регистрацию!");
            return;
        }

        try {
            BigDecimal total = tariffService.calculateTariff(registration.getId());
            response.setValue("calculated_amount", total);
            System.out.println("calculated amount" + total);

        } catch (Exception e) {
            response.setError(e.getMessage());
        }
    }
}
