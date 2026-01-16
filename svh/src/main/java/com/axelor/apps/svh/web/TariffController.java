package com.axelor.apps.svh.web;

import com.axelor.apps.svh.service.TariffService;
import com.google.inject.Inject;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class TariffController {

    @Inject
    private TariffService tariffService;

    @Transactional
    public void calculateTariff(ActionRequest request, ActionResponse response) {

        Object transportObj = request.getContext().get("transport_type"); // select поле
        Object createdObj = request.getContext().get("createdOn"); // дата приезда
        BigDecimal weight = (BigDecimal) request.getContext().get("weight");

        System.out.println("transportObject " + transportObj);

        // Проверка transportType
        if (!(transportObj instanceof String) || ((String) transportObj).isBlank()) {
            response.setError("Заполните тип транспорта");
            return;
        }
        String transportType = (String) transportObj;

        // Проверка createdOn
        if (!(createdObj instanceof LocalDateTime)) {
            response.setError("Неверный формат даты createdOn");
            return;
        }
        LocalDate createdDate = ((LocalDateTime) createdObj).toLocalDate();

        // 1️⃣ Количество дней = difference between createdOn и today
        LocalDate today = LocalDate.now();
        long daysBetween = ChronoUnit.DAYS.between(createdDate, today);
        int days = (int) (daysBetween == 0 ? 1 : daysBetween + 1);

        try {
            BigDecimal totalPrice = tariffService.calculateTariff(transportType, days, weight);
            System.out.println("calculated_amount" + totalPrice);

            response.setValue("calculated_amount", totalPrice);

        } catch (IllegalArgumentException e) {
            response.setError(e.getMessage());
        } catch (Exception e) {
            response.setError("Ошибка при расчёте тарифа: " + e.getMessage());
        }
    }
}
