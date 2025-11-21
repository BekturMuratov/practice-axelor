package com.axelor.apps.svh.web;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.service.RegistrationService;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RegistrationController {

  @Inject RegistrationService registrationService;

  public void calculate(ActionRequest request, ActionResponse response) {
    Registration registration = request.getContext().asType(Registration.class);

    try {
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
