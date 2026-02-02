package com.axelor.apps.svh.service.impl;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.Services;
import com.axelor.apps.svh.service.ServicesService;

import java.math.BigDecimal;

public class ServiceImpl implements ServicesService {


    @Override
    public BigDecimal calculationServiceAmount(Registration registration) {
        if (registration.getServices() == null) {
            return BigDecimal.ZERO;
        }

        return registration.getServices().stream()
                .filter(s -> s.getPrice_for_service() != null && s.getAmount() != null)
                .map(s -> s.getPrice_for_service().multiply(s.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isValidService(Services service) {
        return service.getPrice_for_service() != null
                && service.getAmount() != null;
    }

    private BigDecimal calculateLineAmount(Services service) {
        return service.getPrice_for_service().multiply(service.getAmount());
    }
}
