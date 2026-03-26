package com.axelor.apps.svh.service;

import com.axelor.apps.svh.db.Tariff;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TariffService {

    BigDecimal calculateTariff(Tariff tariff, int days, BigDecimal weight, String transportType);

    int calculateStorageDays(LocalDateTime createdOn, LocalDateTime releasedOn);

    BigDecimal calculateTariffByRegistration(Long registrationId);
}