package com.axelor.apps.svh.service.impl;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.Tariffs;
import com.axelor.apps.svh.db.repo.RegistrationRepository;
import com.axelor.apps.svh.db.repo.TariffsRepository;
import com.axelor.apps.svh.service.CrudService;
import com.axelor.apps.svh.service.RegistrationService;
import com.google.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class RegistrationServiceImpl implements RegistrationService {

    private final CrudService crudService;
    private final TariffsRepository tariffsRepository;
    private final RegistrationRepository registrationRepository;

    @Inject
    public RegistrationServiceImpl(
            CrudService crudService,
            TariffsRepository tariffsRepository,
            RegistrationRepository registrationRepository) {
        this.crudService = crudService;
        this.tariffsRepository = tariffsRepository;
        this.registrationRepository = registrationRepository;
    }

    public static final class TransportType {
        private TransportType() {}
        public static final String FREIGHT = "freight_transport";
        public static final String SPECIAL_EQUIPMENT = "special_equipment";
        public static final String CAR_CARRIER = "car_carrier";
        public static final String PASSENGER = "passenger_transport";
        public static final String COMPANY_CAR = "company_car";
    }


    @Override
    public BigDecimal calculate(Registration registration) {

        Tariffs tariff = registration.getTariff();
        if (tariff == null) {
            throw new IllegalArgumentException("Please select a tariff before calculation.");
        }

        if (registration.getCreatedOn() == null) {
            throw new IllegalArgumentException("createdOn date is required for calculation");
        }

        LocalDateTime now = LocalDateTime.now();

        long actualDays = ChronoUnit.DAYS.between(
                registration.getCreatedOn().toLocalDate(),
                now.toLocalDate()
        );
        if (actualDays <= 0) actualDays = 1;

        System.out.println("Using tariff: " + tariff.getName() +
                ", entryType: " + tariff.getEntry_type() +
                ", basePrice: " + tariff.getBase_price() +
                ", nextDaysPrice: " + tariff.getNext_days_price());

        BigDecimal result;

        switch (tariff.getEntry_type()) {
            case "car":
                result = calculateForCar(tariff, actualDays);
                break;

            case "goods":
                result = calculateForGoods(registration, tariff, actualDays);
                break;

            default:
                throw new IllegalStateException("Unsupported tariff entry type: " + tariff.getEntry_type());
        }

        registration.setCalculated_amount(result);
        crudService.persistObject(registration);

        return result;
    }


    private BigDecimal calculateForCar(Tariffs tariffs, long actualDays) {
        BigDecimal base = toDecimal(tariffs.getBase_price());
        BigDecimal next = toDecimal(tariffs.getNext_days_price());
        int baseDays = tariffs.getBase_days() != null ? tariffs.getBase_days() : 0;
        return actualDays > baseDays ? base.add(next.multiply(BigDecimal.valueOf(actualDays - baseDays))) : base;
    }

    private BigDecimal calculateForGoods(Registration registration, Tariffs tariffs, long actualDays) {
        BigDecimal base = toDecimal(tariffs.getBase_price());
        BigDecimal next = toDecimal(tariffs.getNext_days_price());
        int baseDays = tariffs.getBase_days() != null ? tariffs.getBase_days() : 0;
        BigDecimal weight = registration.getWeight();
        if (weight == null) {
            throw new IllegalArgumentException("Weight is required for goods tariffs");
        }
        BigDecimal result = base.multiply(weight);
        if (actualDays > baseDays) result = result.add(next.multiply(BigDecimal.valueOf(actualDays - baseDays)));
        return result;
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        String v = value.toString().trim().replace(',', '.');
        try {
            return new BigDecimal(v);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid decimal value: " + v);
        }
    }

    public Tariffs getTariffForRegistration(Registration registration) {
        String transportKey = registration.getTransport_type();
        if (transportKey == null) throw new IllegalArgumentException("Unknown transport type");

        String expectedEntryType;
        switch (transportKey) {
            case TransportType.PASSENGER:
            case TransportType.COMPANY_CAR:
            case TransportType.CAR_CARRIER:
                expectedEntryType = "car";
                break;
            case TransportType.FREIGHT:
            case TransportType.SPECIAL_EQUIPMENT:
                expectedEntryType = "goods";
                break;
            default:
                throw new IllegalArgumentException("Unsupported transport type: " + transportKey);
        }

        LocalDateTime now = LocalDateTime.now();

        return tariffsRepository.all().fetch().stream()
                .filter(t -> expectedEntryType.equals(t.getEntry_type())
                        && t.getStart_date() != null
                        && (t.getEnd_date() == null || !now.isAfter(t.getEnd_date())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No matching tariff found for transport type: " + transportKey));
    }


}
