package com.axelor.apps.svh.service;

import com.axelor.apps.svh.db.Registration;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RegistrationService {
    BigDecimal getTotalBetween(LocalDate from, LocalDate to);
    BigDecimal calculateTotalForRegistration(Registration registration);
}
