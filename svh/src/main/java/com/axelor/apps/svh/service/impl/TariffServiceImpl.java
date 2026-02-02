package com.axelor.apps.svh.service.impl;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.Tariff;
import com.axelor.apps.svh.db.repo.RegistrationRepository;
import com.axelor.apps.svh.db.repo.TariffRepository;
import com.axelor.apps.svh.service.TariffService;
import com.axelor.apps.svh.utils.TransportTypes;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class TariffServiceImpl implements TariffService {

    private static final Logger logger = LoggerFactory.getLogger(TariffServiceImpl.class);

    @Inject
    private RegistrationRepository registrationRepository;

    @Inject
    private TariffRepository tariffRepository;

    @Override
    public BigDecimal calculateTariff(Long registrationId) {

        if (registrationId == null) {
            throw new IllegalArgumentException("registrationId не может быть null");
        }

        Registration registration = registrationRepository.find(registrationId);
        if (registration == null) {
            throw new IllegalArgumentException("Registration не найден");
        }

        String transportType = registration.getTransport_type();
        if (transportType == null || transportType.isBlank()) {
            throw new IllegalArgumentException("Transport type не заполнен");
        }

        // --- ДАТЫ ---
        LocalDate createdDate = registration.getCreatedOn().toLocalDate();
        LocalDate releasedDate = registration.getReleasedOn() != null
                ? registration.getReleasedOn().toLocalDate()
                : LocalDate.now();

        long days = ChronoUnit.DAYS.between(createdDate, releasedDate) + 1;
        if (days <= 0) {
            throw new IllegalArgumentException("Некорректный диапазон дат");
        }

        logger.debug("Registration ID: {}", registrationId);
        logger.debug("Transport type: {}", transportType);
        logger.debug("Created: {}, Released: {}, Days: {}", createdDate, releasedDate, days);

        // --- СЛУЖЕБНОЕ АВТО ---
        if (TransportTypes.COMPANY_CAR.equals(transportType)) {
            return BigDecimal.ZERO;
        }

        // --- ТАРИФЫ ПО ТИПУ ---
        List<Tariff> tariffs = tariffRepository.all()
                .filter("self.name = ?", transportType)
                .fetch();

        if (tariffs.isEmpty()) {
            throw new IllegalStateException("Нет тарифов для типа: " + transportType);
        }

        // --- ТАРИФ ПО ДАТЕ ---
        Tariff tariff = tariffs.stream()
                .filter(t -> t.getStartDateTime() != null && t.getEndDateTime() != null)
                .filter(t -> {
                    LocalDate start = t.getStartDateTime().toLocalDate();
                    LocalDate end = t.getEndDateTime().toLocalDate();
                    return !createdDate.isBefore(start) && !createdDate.isAfter(end);
                })
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("Нет действующего тарифа на дату: " + createdDate)
                );

        logger.debug("Selected tariff: {}", tariff);

        BigDecimal total = tariff.getPrice_per_day()
                .multiply(BigDecimal.valueOf(days));

        // --- ВЕС ТОЛЬКО ДЛЯ FREIGHT ---
        if (TransportTypes.FREIGHT.equals(transportType)) {
            BigDecimal weight = registration.getWeight();
            if (weight == null) {
                throw new IllegalStateException("Вес обязателен для FREIGHT");
            }

            if (tariff.getMin_weight() != null && weight.compareTo(tariff.getMin_weight()) < 0 ||
                    tariff.getMax_weight() != null && weight.compareTo(tariff.getMax_weight()) > 0) {
                throw new IllegalStateException("Вес вне диапазона тарифа: " + weight);
            }

            total = total.multiply(weight);
        }

        logger.debug("Total amount: {}", total);
        return total;
    }
}
