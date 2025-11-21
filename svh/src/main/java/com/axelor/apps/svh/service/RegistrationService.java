package com.axelor.apps.svh.service;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.Tariffs;

import java.math.BigDecimal;

public interface RegistrationService {
  BigDecimal calculate(Registration registration);
  Tariffs getTariffForRegistration(Registration registration);

}
