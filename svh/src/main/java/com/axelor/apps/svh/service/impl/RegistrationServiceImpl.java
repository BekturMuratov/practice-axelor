package com.axelor.apps.svh.service.impl;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.Tariffs;
import com.axelor.apps.svh.db.repo.RegistrationRepository;
import com.axelor.apps.svh.db.repo.TariffsRepository;
import com.axelor.apps.svh.service.CrudService;
import com.axelor.apps.svh.service.RegistrationService;
import com.google.inject.Inject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class RegistrationServiceImpl implements RegistrationService {

    private final CrudService crudService;
    private final TariffsRepository tariffsRepository;
    private final RegistrationRepository registrationRepository;

    private static final Map<String, String> TRANSPORT_MAP = Map.of(
            "passenger_transport", new String("Легковое авто".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
            "freight_transport", new String("Грузовое авто".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
            "special_equipment", new String("Спецтехника".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
            "car_carrier", new String("Автовоз".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
            "company_car", new String("Служебное авто".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
    );


    @Inject
    public RegistrationServiceImpl(
            CrudService crudService,
            TariffsRepository tariffsRepository,
            RegistrationRepository registrationRepository) {
        this.crudService = crudService;
        this.tariffsRepository = tariffsRepository;
        this.registrationRepository = registrationRepository;
    }


    @Override
    public BigDecimal calculate(Registration registration) {
        // 1. Берем transport_type из регистрации (английский ключ)
        String transportKey = registration.getTransport_type();
        if (transportKey == null) {
            throw new IllegalArgumentException("Unknown transport type: " + transportKey);
        }

        // 2. Получаем русское название для сравнения с Tariffs.name
        String transportLabel = TRANSPORT_MAP.get(transportKey);
        if (transportLabel == null) {
            throw new IllegalArgumentException("No Russian label for transport type: " + transportKey);
        }
        final String transportLabelTrimmed = transportLabel.trim();

        // 3. Получаем все тарифы из базы
        var tariffsList = tariffsRepository.all().fetch();

        // 4. Находим тариф, сравнивая с русским названием
        Tariffs tariff = tariffsList.stream()
                .filter(t -> {
                    String name = t.getName();
                    if (name != null) {
                        byte[] bytes = name.getBytes(StandardCharsets.ISO_8859_1); // raw bytes из базы
                        String nameUtf8 = new String(bytes, StandardCharsets.UTF_8);
                        System.out.println("Tariff in UTF-8: [" + nameUtf8 + "]");
                    }
                    // вывод для дебага
                    System.out.println("Tariff in DB: [" + name + "]");
                    System.out.println("Transport label: [" + transportLabelTrimmed + "]");

                    LocalDateTime now = LocalDateTime.now();
                    return name.equals(transportLabelTrimmed)
                            && t.getStart_date() != null
                            && t.getEnd_date() != null
                            && !now.isBefore(t.getStart_date())
                            && !now.isAfter(t.getEnd_date());
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tariff not found for transport type: " + transportLabelTrimmed));

        // 5. Проверка даты создания
        if (registration.getCreatedOn() == null) {
            throw new IllegalArgumentException("createdOn date is required for calculation");
        }

        // 6. Вычисляем количество дней
        long actualDays = ChronoUnit.DAYS.between(
                registration.getCreatedOn().toLocalDate(),
                LocalDateTime.now().toLocalDate()
        );
        if (actualDays == 0) actualDays = 1;

        // 7. Выбор расчета по entry_type
        BigDecimal result;
        switch (tariff.getEntry_type()) {
            case "car":
                result = calculateForCar(tariff, actualDays);
                break;
            case "goods":
                result = calculateForGoods(registration, tariff, actualDays);
                break;
            default:
                throw new IllegalStateException(
                        "Unsupported tariff entry type: " + tariff.getEntry_type());
        }

        // 8. Сохраняем результат
        registration.setCalculated_amount(result);
        crudService.persistObject(registration);

        return result;
    }



    private BigDecimal calculateForCar(Tariffs tariffs, long actualDays) {
        BigDecimal base = toDecimal(tariffs.getBase_price());
        BigDecimal next = toDecimal(tariffs.getNext_days_price());
        int baseDays = tariffs.getBase_days() != null ? tariffs.getBase_days() : 0;

        if (actualDays > baseDays) {
            return base.add(next.multiply(BigDecimal.valueOf(actualDays - baseDays)));
        }
        return base;
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
        if (actualDays > baseDays) {
            result = result.add(next.multiply(BigDecimal.valueOf(actualDays - baseDays)));
        }
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

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

}
