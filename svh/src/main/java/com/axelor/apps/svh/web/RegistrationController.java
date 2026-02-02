package com.axelor.apps.svh.web;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.Services;
import com.axelor.apps.svh.db.repo.RegistrationRepository;
import com.axelor.apps.svh.service.RegistrationService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RegistrationController {

    @Inject
    private RegistrationRepository registrationRepository;

    @Inject
    private RegistrationService registrationService;
                                                                                                                        //Устанавливает дату создания при первом сохранении
    public void setCurrentDate(ActionRequest request, ActionResponse response) {
        Registration registration =
                request.getContext().asType(Registration.class);

        if (registration.getCreatedOn() == null) {
            registration.setCreatedOn(LocalDateTime.now());
            response.setValue("createdOn", registration.getCreatedOn());
        }
    }
                                                                                                                         //Нормализация полей при вводе (onChange)
                                                                                                                         // Без ошибок и валидаций — только UX
    public void normalizeOnChange(ActionRequest request, ActionResponse response) {

        Registration registration =
                request.getContext().asType(Registration.class);

        response.setValue("plate_no",
                normalize(registration.getPlate_no()));

        response.setValue("plate_no_trailer",
                normalize(registration.getPlate_no_trailer()));

        response.setValue("serial_number",
                normalize(registration.getSerial_number()));

        response.setValue("vin_code",
                normalize(registration.getVin_code()));
    }
                                                                                                                         //Финальная нормализация и валидация при сохранении (onSave)
    public void normalizeAndValidateOnSave(
            ActionRequest request,
            ActionResponse response) {

        Registration registration =
                request.getContext().asType(Registration.class);

        registration.setPlate_no(
                normalize(registration.getPlate_no()));

        registration.setPlate_no_trailer(
                normalize(registration.getPlate_no_trailer()));

        registration.setSerial_number(
                normalize(registration.getSerial_number()));

        String vin = normalize(registration.getVin_code());

        if (vin != null && vin.length() != 17) {
            response.setError("VIN должен содержать ровно 17 символов");
            return;
        }

        registration.setVin_code(vin);

        response.setValues(registration);
    }

                                                                                                                        //Универсальная нормализация:
                                                                                                                        //     * - trim
                                                                                                                        //     * - убрать все пробелы
                                                                                                                        //     * - UPPER CASE
    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value
                .trim()
                .replaceAll("\\s+", "")
                .toUpperCase();
    }


    public void loadDashboardStats(
            ActionRequest request,
            ActionResponse response
    ) {
        RegistrationService service = Beans.get(RegistrationService.class);

        LocalDate today = LocalDate.now();

        response.setValue("today_total", service.getTotalBetween(today, today));

        response.setValue("week_total", service.getTotalBetween(
                today.with(DayOfWeek.MONDAY),
                today
        ));

        response.setValue("month_total", service.getTotalBetween(
                today.withDayOfMonth(1),
                today
        ));

        response.setValue("year_total", service.getTotalBetween(
                today.withDayOfYear(1),
                today
        ));
    }


    public void calculateTotal(ActionRequest request, ActionResponse response) {
        Registration registration = request.getContext().asType(Registration.class);

        if(registration == null || registration.getId() == null) {
            response.setNotify("Сначала сохраните регистрацию");
            return;
        }

        Registration regFromDb = registrationRepository.find(registration.getId());

        if(regFromDb == null) {
            response.setError("Registration не найден");
            return;
        }

        try {
            BigDecimal totalAmount = registrationService.calculateTotalForRegistration(regFromDb);

            response.setValue("total_amount", totalAmount);
        }
        catch (IllegalArgumentException | IllegalStateException e) {
            response.setNotify(e.getMessage());
        }


    }
}
