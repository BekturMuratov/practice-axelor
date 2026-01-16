package com.axelor.apps.svh.service.impl;


import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.repo.RegistrationRepository;
import com.axelor.apps.svh.service.RegistrationService;
import com.google.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistrationServiceImpl implements RegistrationService {
    @Inject
    RegistrationRepository registrationRepository;


    @Override
    public BigDecimal getTotalBetween(LocalDate from, LocalDate to) {
        return registrationRepository.all().filter(
                "self.createdOn >= ? AND self.createdOn < ?",
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()
        ).fetchStream()
                .map(Registration::getCalculated_amount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    @Override
    public Map<LocalDate, BigDecimal> getDailyTotals(LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> result = new HashMap<>();

        List<Registration> list = registrationRepository.all()
                .filter(
                        "self.createdOn >= ? AND self.createdOn < ?",
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay()
                ).fetch();

        for(Registration r : list) {
            if(r.getCalculated_amount() == null) continue;

            LocalDate day = r.getCreatedOn().toLocalDate();

            result.merge(
                    day,
                    r.getCalculated_amount(),
                    BigDecimal::add
            );
        }
        return result;
    }
}
