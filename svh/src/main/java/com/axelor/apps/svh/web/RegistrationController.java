package com.axelor.apps.svh.web;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.Tariffs;
import com.axelor.apps.svh.db.repo.RegistrationRepository;
import com.axelor.apps.svh.db.repo.TariffsRepository;
import com.axelor.apps.svh.service.RegistrationService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RegistrationController {

  @Inject
  RegistrationService registrationService;
  RegistrationRepository registrationRepository;
  TariffsRepository tariffsRepository;


    public void calculate(ActionRequest request, ActionResponse response) {

        Registration ctxReg = request.getContext().asType(Registration.class);

        try {

            // если id == null — пользователь не сохранял форму
            Registration registration;
            if (ctxReg.getId() == null) {
                registration = ctxReg;  // работаем прямо с тем, что пришло
            } else {
                registration = registrationRepository.find(ctxReg.getId());
            }

            // тариф тоже может быть null
            if (ctxReg.getTariff() == null || ctxReg.getTariff().getId() == null) {
                response.setError("Please select a tariff");
                return;
            }

            // подгружаем тариф
            Tariffs selectedTariff = tariffsRepository.find(ctxReg.getTariff().getId());

            registration.setTariff(selectedTariff);

            BigDecimal amount = registrationService.calculate(registration);

            response.setValue("calculated_amount", amount);
            response.setNotify("Amount calculated successfully");

        } catch (Exception e) {
            response.setError("Error: " + e.getMessage());
        }
    }




    public void setCurrentDate(ActionRequest request, ActionResponse response) {
    Registration registration = request.getContext().asType(Registration.class);

    if (registration.getCreatedOn() == null) {
      registration.setCreatedOn(LocalDateTime.now());
    }

    response.setValue("createdOn", registration.getCreatedOn());
  }
}
