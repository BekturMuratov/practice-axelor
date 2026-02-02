package com.axelor.apps.svh.service.impl;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.Services;
import com.axelor.apps.svh.service.ServicesService;

import java.math.BigDecimal;
import java.util.List;

public class ServiceImpl implements ServicesService {

    @Override
    public BigDecimal calculationServiceAmount(Registration registration) {

        if (registration == null || registration.getServices() == null) {
            return BigDecimal.ZERO;
        }

        return registration.getServices().stream()
                .map(Services::getPrice_for_service)              // берем amount
                .filter(amount -> amount != null)      // защита от null
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
