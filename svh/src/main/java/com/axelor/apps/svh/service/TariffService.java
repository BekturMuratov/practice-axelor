package com.axelor.apps.svh.service;

import java.math.BigDecimal;

public interface TariffService {
  BigDecimal calculateTariff(String transportType, Integer days, BigDecimal weight);
}
