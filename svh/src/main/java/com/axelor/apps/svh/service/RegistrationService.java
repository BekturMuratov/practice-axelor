package com.axelor.apps.svh.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RegistrationService {
    BigDecimal getTotalBetween(LocalDate from, LocalDate to);
}
