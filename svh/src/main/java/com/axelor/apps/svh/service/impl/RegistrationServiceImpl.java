package com.axelor.apps.svh.service.impl;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.Services;
import com.axelor.apps.svh.db.repo.RegistrationRepository;
import com.axelor.apps.svh.service.RegistrationService;
import com.google.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class RegistrationServiceImpl implements RegistrationService {

    @Inject
    RegistrationRepository registrationRepository;

    @Override
    public BigDecimal getTotalBetween(LocalDate from, LocalDate to) {
        return registrationRepository.all().filter("self.createdOn >= ? AND self.createdOn < ?",
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay()).fetchStream()
                .map(Registration::getCalculated_amount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalForRegistration(Registration registration) {

        if(registration == null) {
            throw new IllegalArgumentException("Registration = null");
        }

        BigDecimal caclulatedAmount = registration.getCalculated_amount();

        if(caclulatedAmount == null) {
            throw new IllegalArgumentException("calculated amount не рассчитан");
        }

        BigDecimal servicesAmount = BigDecimal.ZERO;

        if(registration.getServices() != null) {
            servicesAmount = registration.getServices().stream()
                    .map(Services::getPrice_for_service)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return caclulatedAmount.add(servicesAmount);
    }


}
