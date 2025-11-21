package com.axelor.apps.svh.service;

import com.axelor.apps.svh.db.Registration;
import java.math.BigDecimal;

public interface RegistrationService {
  BigDecimal calculate(Registration registration);
}
