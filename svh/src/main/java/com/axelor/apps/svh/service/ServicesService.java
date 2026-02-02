package com.axelor.apps.svh.service;

import com.axelor.apps.svh.db.Registration;

import java.math.BigDecimal;

public interface ServicesService {
    BigDecimal calculationServiceAmount(Registration registration);
}
