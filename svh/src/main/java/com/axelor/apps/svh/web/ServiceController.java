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
    private ServicesService servicesService;

    @Inject
    private RegistrationRepository registrationRepository;

    public void calculationService(ActionRequest request, ActionResponse response) {

        // 1. Получаем Registration из UI
        Registration registration = request.getContext().asType(Registration.class);

        if (registration == null || registration.getId() == null) {
            response.setNotify("Сначала сохраните регистрацию");
            return;
        }

        // 2. Загружаем из БД (ВАЖНО!)
        Registration regFromDb = registrationRepository.find(registration.getId());


        if (regFromDb == null) {
            response.setError("Registration не найден");
            return;
        }

        if (regFromDb.getServices() == null || regFromDb.getServices().isEmpty()) {
            response.setNotify("У регистрации нет услуг");
            response.setValue("services_amount", BigDecimal.ZERO);
            return;
        }

        // 3. Считаем сумму услуг
        BigDecimal servicesAmount =
                servicesService.calculationServiceAmount(regFromDb);

        // 4. Записываем в форму Registration
        response.setValue("services_amount", servicesAmount);
    }
}
