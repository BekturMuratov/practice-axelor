package com.axelor.apps.svh.service.impl;

import com.axelor.apps.svh.db.Tariff;
import com.axelor.apps.svh.db.TariffRule;
import com.axelor.apps.svh.db.repo.TariffRepository;
import com.axelor.apps.svh.service.TariffService;
import com.google.inject.Inject;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.axelor.apps.svh.utils.TransportTypes.*;

public class TariffServiceImpl implements TariffService {

    @Inject
    private TariffRepository tariffRepository;

    @Override
    @Transactional
    public BigDecimal calculateTariff(
            String transportType,
            Integer days,
            BigDecimal weight) {

        if (transportType == null || transportType.isBlank()) {
            throw new IllegalArgumentException("Transport type must not be empty");
        }

        if (days == null || days <= 0) {
            throw new IllegalArgumentException("Days must be greater than 0");
        }

                                                                                                                         // Служебное авто — всегда бесплатно
        if (COMPANY_CAR.equals(transportType)) {
            return BigDecimal.ZERO;
        }

                                                                                                                         // Получаем тариф по коду (FREIGHT, CAR_CARRIER и т.д.)
        Tariff tariff = tariffRepository.all()
                .filter("self.name = ?", transportType)
                .fetchOne();

        if (tariff == null) {
            throw new IllegalStateException(
                    "Tariff not found for transport type: " + transportType
            );
        }

                                                                                                                         //  Получаем и сортируем правила
        List<TariffRule> rules = tariff.getTariff_rule()
                .stream()
                .sorted(Comparator.comparing(TariffRule::getStart_day))
                .collect(Collectors.toList());

        if (rules.isEmpty()) {
            throw new IllegalStateException(
                    "No rules defined for tariff: " + transportType
            );
        }

        BigDecimal total = BigDecimal.ZERO;

                                                                                                                         // Применяем правила по календарным дням
        for (TariffRule rule : rules) {
            if (!matchWeight(rule, weight)) {
                continue;
            }
            int startDay = rule.getStart_day();
            int endDay = rule.getEnd_day();

                                                                                                                         // Сколько дней реально попадает в диапазон
            int appliedDays =
                    Math.min(endDay, days)
                            - Math.max(startDay, 1)
                            + 1;

            if (appliedDays <= 0) {
                continue;
            }

            BigDecimal dayPrice = rule.getPrice_per_day();

                                                                                                                         //  Логика по типам транспорта
            switch (transportType) {

                case FREIGHT:
                    if (weight == null) {
                        throw new IllegalArgumentException(
                                "Weight must be provided for FREIGHT"
                        );
                    }
                    dayPrice = dayPrice.multiply(weight);
                    break;

                case SPECIAL:
                case CAR_CARRIER:
                case PASSENGER:
                                                                                                                        // ставка фиксированная
                    break;
                default:
                    throw new IllegalStateException(
                            "Unsupported transport type: " + transportType
                    );
            }

            total = total.add(
                    dayPrice.multiply(
                            BigDecimal.valueOf(appliedDays)
                    )
            );
        }

        return total;
    }

                                                                                                                         //Проверка соответствия правила весу
    private boolean matchWeight(
            TariffRule rule,
            BigDecimal weight) {

        if (weight == null) {
            return true;
        }

        if (rule.getMin_weight() != null &&
                weight.compareTo(rule.getMin_weight()) < 0) {
            return false;
        }

        if (rule.getMax_weight() != null &&
                weight.compareTo(rule.getMax_weight()) > 0) {
            return false;
        }

        return true;
    }
}
