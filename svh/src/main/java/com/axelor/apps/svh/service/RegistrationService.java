package com.axelor.apps.svh.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface RegistrationService {

    BigDecimal getTotalBetween(
            LocalDate from,
            LocalDate to
    );


}
