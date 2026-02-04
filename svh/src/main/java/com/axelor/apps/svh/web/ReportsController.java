package com.axelor.apps.svh.web;

import com.axelor.apps.svh.service.RegistrationService;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import net.bytebuddy.asm.Advice;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class ReportsController {

    @Inject
    RegistrationService registrationService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public void getTotalCars(ActionRequest request, ActionResponse response) {
       try {
           String fromStr = request.getContext().get("report_start_date") != null ?
                   request.getContext().get("report_start_date").toString() : null;

           String toStr = request.getContext().get("report_end_date") != null ?
                   request.getContext().get("report_end_date").toString() : null;

           if(fromStr == null || toStr == null) {
               response.setError("Report start date и Report end date должны быть указаны");
           }

           LocalDate from = parseDate(fromStr);
           LocalDate to = parseDate(toStr);

           BigDecimal totalCars = registrationService.getAllTotalOfCars(from, to);
           response.setValue("all_cars_total", totalCars);
           System.out.println("all_cars_total: " + totalCars);
       } catch (Exception e) {
           response.setError("Ошибка при расчете totalCars: " + e.getMessage());
       }
    }


    public void getTotalServices(ActionRequest request, ActionResponse response) {
      try {
          String fromStr = request.getContext().get("report_start_date") != null ?
                  request.getContext().get("report_start_date").toString() : null;

          String toStr = request.getContext().get("report_end_date") != null ?
                  request.getContext().get("report_end_date").toString() : null;


          if(fromStr == null || toStr == null) {
              response.setError("Report start date и Report end date должны быть указаны");
              return;
          }


          LocalDate from = parseDate(fromStr);
          LocalDate to = parseDate(toStr);

          BigDecimal totalServices = registrationService.getAllTotalOfServices(from, to);
          response.setValue("all_services_total", totalServices);
          System.out.println("all services total" + totalServices);
      } catch (Exception e) {
          response.setError("Ошибка при расчете totalServices: " + e.getMessage());
      }
    }

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void exportRegistrations(ActionRequest request, ActionResponse response) {
        try {
            // --- даты из формы ---
            LocalDate from = getDate(request, "report_start_date");
            LocalDate to   = getDate(request, "report_end_date");

            // --- папка Загрузки пользователя ---
            String userHome = System.getProperty("user.home");
            Path downloadsDir = Path.of(userHome, "Downloads");

            if (!Files.exists(downloadsDir)) {
                Files.createDirectories(downloadsDir);
            }

            // --- формат даты и времени ---
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH-mm-ss");

            // --- имя файла ---
            String fileName = String.format(
                    "Отчет_за_период_%s_по_%s_%s.xlsx",
                    from.format(dateFormatter),
                    to.format(dateFormatter),
                    LocalDateTime.now().format(timeFormatter)
            );

            Path filePath = downloadsDir.resolve(fileName);

            // --- генерация Excel ---
            try (OutputStream os = new FileOutputStream(filePath.toFile())) {
                registrationService.exportRegistrationsToExcel(from, to, os);
            }

            // --- уведомление пользователю ---
            response.setExportFile(filePath.toString());
            response.setNotify("Отчет успешно сохранён: " + fileName);

            System.out.println("Excel report saved to: " + filePath);

        } catch (Exception e) {
            response.setError("Ошибка экспорта отчета: " + e.getMessage());
            e.printStackTrace();
        }
    }




    private LocalDate getDate(ActionRequest request, String field) {
        Object value = request.getContext().get(field);

        if(value == null) {
            throw new IllegalArgumentException(field + " должен быть заполнен");
        }

        String dateStr = value.toString();


        try {
            return LocalDateTime.parse(dateStr).toLocalDate();
        } catch (DateTimeParseException e) {
            return  LocalDate.parse(dateStr);
        }

    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr).toLocalDate();
        } catch (DateTimeParseException e) {
            return LocalDate.parse(dateStr);
        }
    }
}
