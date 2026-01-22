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

        Object transportObj = request.getContext().get("transport_type");
        Object createdObj = request.getContext().get("createdOn");
        BigDecimal weight = (BigDecimal) request.getContext().get("weight");

        if (transportObj == null || transportObj.toString().isBlank()) {
            response.setError("Заполните тип транспорта");
            return;
        }
        String transportType = transportObj.toString();

        LocalDate createdDate;
        if (createdObj instanceof LocalDateTime) {
            createdDate = ((LocalDateTime) createdObj).toLocalDate();
        } else {
            createdDate = LocalDate.now();
        }

        LocalDate today = LocalDate.now();
        long daysBetween = ChronoUnit.DAYS.between(createdDate, today);

        if (daysBetween < 0) {
            response.setError("Дата создания не может быть в будущем");
            return;
        }

        int days = (int) daysBetween + 1;

        try {
            BigDecimal totalPrice =
                    tariffService.calculateTariff(transportType, days, weight);

            response.setValue("calculated_amount", totalPrice);

        } catch (IllegalArgumentException e) {
            response.setError(e.getMessage());
        } catch (Exception e) {
            response.setError("Ошибка при расчёте тарифа");
        }
    }
}
