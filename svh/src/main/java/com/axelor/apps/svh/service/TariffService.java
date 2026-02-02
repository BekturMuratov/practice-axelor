package com.axelor.apps.svh.service;

import java.math.BigDecimal;

public interface TariffService {

    BigDecimal calculateTariff(Long registrationId);
}