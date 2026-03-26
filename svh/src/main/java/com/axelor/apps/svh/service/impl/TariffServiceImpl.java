package com.axelor.apps.svh.service.impl;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.Tariff;
import com.axelor.apps.svh.db.repo.RegistrationRepository;
import com.axelor.apps.svh.db.repo.TariffRepository;
import com.axelor.apps.svh.service.TariffService;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.axelor.apps.svh.utils.StatusConstants.COMPANY_CAR;
import static com.axelor.apps.svh.utils.StatusConstants.TRUCK;

public class TariffServiceImpl implements TariffService {

    private static final Logger logger = LoggerFactory.getLogger(TariffServiceImpl.class);

    @Inject
    RegistrationRepository registrationRepository;

    @Inject
    TariffRepository tariffRepository;

    @Override
    public BigDecimal calculateTariff(Tariff tariff, int days, BigDecimal weight, String transportType) {
        if(tariff == null) {
            return  BigDecimal.ZERO;
        }

        int baseDays = tariff.getBaseDays();

        if(COMPANY_CAR.equals(transportType)) {
            return BigDecimal.ZERO;
        }

        if(!TRUCK.equals(transportType)) {
            BigDecimal dailyPrice = tariff.getDailyPrice();
            BigDecimal afterPrice = tariff.getPriceAfterChange();

            if (days <= baseDays) {
                return dailyPrice.multiply(BigDecimal.valueOf(days));
            }


            int extraDays = days - baseDays;

            return dailyPrice.multiply(BigDecimal.valueOf(baseDays))
                    .add(afterPrice.multiply(BigDecimal.valueOf(extraDays)));
        }

        BigDecimal threshold = tariff.getMaxWeight();

        BigDecimal baseRate;
        BigDecimal afterRate;

        BigDecimal weightBD = weight;

        if (weightBD.compareTo(threshold) < 0) {
            baseRate = tariff.getPriceUpToWeight();
            afterRate = tariff.getPriceUpToWeightAfter();
        } else {
            baseRate = tariff.getPriceAboveWeight();
            afterRate = tariff.getPriceAboveWeightAfter();
        }

        BigDecimal daysBD = BigDecimal.valueOf(days);
        BigDecimal baseDaysBD = BigDecimal.valueOf(baseDays);

        if (days <= baseDays) {
            return baseRate
                    .multiply(weightBD)
                    .multiply(daysBD);
        }

        int extraDays = days - baseDays;
        BigDecimal extraDaysBD = BigDecimal.valueOf(extraDays);

        return baseRate.multiply(weightBD).multiply(baseDaysBD)
                .add(afterRate.multiply(weightBD).multiply(extraDaysBD));
    }

    @Override
    public int calculateStorageDays(LocalDateTime createdOn, LocalDateTime releasedOn) {

        if (createdOn == null) {
            return 0;
        }

        LocalDateTime endDate = releasedOn != null ? releasedOn : LocalDateTime.now();

        long days = ChronoUnit.DAYS.between(createdOn, endDate);

        if (days <= 0) {
            return 1;
        }

        return (int) days;
    }

    @Override
    public BigDecimal calculateTariffByRegistration(Long registrationId) {
        Registration registration = registrationRepository.find(registrationId);

        String transportType = registration.getTransport_type();
        Tariff tariff = tariffRepository
                .all()
                .filter("self.tariffType =?1", transportType)
                .fetchOne();

        if(registration == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal weight = registration.getWeight();

        if(weight == null) {
            return BigDecimal.ZERO;
        }

        int days = calculateStorageDays(
                registration.getCreatedOn(),
                registration.getReleasedOn()
        );

        return calculateTariff(tariff, days, weight, transportType);
    }
}
