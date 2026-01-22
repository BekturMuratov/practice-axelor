package com.axelor.apps.svh.service.impl;

import com.axelor.apps.svh.db.Tariff;
import com.axelor.apps.svh.db.repo.TariffRepository;
import com.axelor.apps.svh.service.TariffService;
import com.google.inject.Inject;

import java.math.BigDecimal;

import static com.axelor.apps.svh.utils.TransportTypes.*;

public class TariffServiceImpl implements TariffService {

    @Inject
    private TariffRepository tariffRepository;

    @Override
    public BigDecimal calculateTariff(
            String transportType,
            Integer days,
            BigDecimal weight) {

        validateInput(transportType, days);

        // Служебный транспорт — бесплатно
        if (COMPANY_CAR.equals(transportType)) {
            return BigDecimal.ZERO;
        }

        Tariff tariff = tariffRepository.all()
                .filter("self.name = ?", transportType)
                .fetchOne();

        if (tariff == null) {
            throw new IllegalStateException(
                    "Tariff not found for transport type: " + transportType
            );
        }

        // Проверка диапазона дней
        if (!matchDays(tariff, days)) {
            throw new IllegalStateException(
                    "Days out of tariff range: " + days
            );
        }

        // Проверка веса
        if (!matchWeight(tariff, weight)) {
            throw new IllegalStateException(
                    "Weight out of tariff range: " + weight
            );
        }

        BigDecimal pricePerDay = tariff.getPrice_per_day();
        BigDecimal total = pricePerDay.multiply(BigDecimal.valueOf(days));

        // Для грузового транспорта — цена зависит от веса
        if (FREIGHT.equals(transportType)) {
            if (weight == null) {
                throw new IllegalArgumentException(
                        "Weight must be provided for FREIGHT transport"
                );
            }
            total = total.multiply(weight);
        }

        return total;
    }

    // =========================
    // Validation helpers
    // =========================

    private void validateInput(String transportType, Integer days) {
        if (transportType == null || transportType.isBlank()) {
            throw new IllegalArgumentException("Transport type must not be empty");
        }

        if (days == null || days <= 0) {
            throw new IllegalArgumentException("Days must be greater than 0");
        }
    }

    private boolean matchDays(Tariff tariff, Integer days) {
        if (tariff.getStart_day() != null && days < tariff.getStart_day()) {
            return false;
        }
        if (tariff.getEnd_day() != null && days > tariff.getEnd_day()) {
            return false;
        }
        return true;
    }

    private boolean matchWeight(Tariff tariff, BigDecimal weight) {
        if (weight == null) {
            return true;
        }

        if (tariff.getMin_weight() != null &&
                weight.compareTo(tariff.getMin_weight()) < 0) {
            return false;
        }

        if (tariff.getMax_weight() != null &&
                weight.compareTo(tariff.getMax_weight()) > 0) {
            return false;
        }
        return true;
    }
}
