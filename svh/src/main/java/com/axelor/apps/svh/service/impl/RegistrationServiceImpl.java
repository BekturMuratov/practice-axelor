package com.axelor.apps.svh.service.impl;

import com.axelor.apps.svh.db.Registration;
import com.axelor.apps.svh.db.Services;
import com.axelor.apps.svh.db.repo.RegistrationRepository;
import com.axelor.apps.svh.service.RegistrationService;
import com.google.inject.Inject;

import javax.persistence.EntityManager;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class RegistrationServiceImpl implements RegistrationService {

    @Inject
    RegistrationRepository registrationRepository;

    @Override
    public BigDecimal getTotalBetween(LocalDate from, LocalDate to) {
        return registrationRepository.all().filter("self.createdOn >= ? AND self.createdOn < ?",
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay()).fetchStream()
                .map(Registration::getCalculated_amount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateTotalForRegistration(Registration registration) {

        if(registration == null) {
            throw new IllegalArgumentException("Registration = null");
        }

        BigDecimal caclulatedAmount = registration.getCalculated_amount();

        if(caclulatedAmount == null) {
            throw new IllegalArgumentException("calculated amount не рассчитан");
        }

        BigDecimal servicesAmount = BigDecimal.ZERO;

        if(registration.getServices() != null) {
            servicesAmount = registration.getServices().stream()
                    .map(Services::getPrice_for_service)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return caclulatedAmount.add(servicesAmount);
    }

    @Override
    public BigDecimal getAllTotalOfCars(LocalDate from, LocalDate to) {
        if(from == null || to == null) {
            throw new IllegalArgumentException("Date range cannot be null");
        }

        return registrationRepository.all()
                .filter("self.createdOn >= ? AND self.createdOn < ?",
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay())
                .fetchStream()
                .filter(Objects::nonNull)
                .map(Registration::getCalculated_amount)
                .filter(amount -> amount != null && amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getAllTotalOfServices(LocalDate from, LocalDate to) {
        if(from == null || to == null) {
            throw new IllegalArgumentException("Date range cannot be null");
        }

       return registrationRepository.all()
               .filter("self.createdOn >= ? AND self.createdOn < ?",
                       from.atStartOfDay(),
                       to.plusDays(1).atStartOfDay())
               .fetch()
               .stream()
               .filter(r -> r.getServices() != null)
               .flatMap(r -> r.getServices().stream())
               .map(s -> s.getPrice_for_service())
               .filter(a -> a != null && a.compareTo(BigDecimal.ZERO) > 0)
               .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void exportRegistrationsToExcel(LocalDate from, LocalDate to, OutputStream outputStream) throws Exception {

        EntityManager em = com.axelor.db.JPA.em();

        List<Registration> registrations = em.createQuery(
                        "SELECT DISTINCT r FROM Registration r " +
                                "LEFT JOIN FETCH r.services s " +
                                "WHERE r.createdOn >= :from AND r.createdOn < :to " +
                                "ORDER BY r.createdOn ASC", Registration.class)
                .setParameter("from", from.atStartOfDay())
                .setParameter("to", to.plusDays(1).atStartOfDay())
                .getResultList();

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Registrations");

        // ===== HEADER =====
        Row header = sheet.createRow(0);
        String[] columns = {
                "Дата регистрации", "Тип АТС", "Гос Номер", "VIN код",
                "Стоимость за хранение АТС", "Наименование услуги", "Стоимость за услугу",
                "Всего за услугу", "Итого"
        };

        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }

        int rowIdx = 1;

        // ===== DATA =====
        for (Registration reg : registrations) {

            // Нет услуг
            if (reg.getServices() == null || reg.getServices().isEmpty()) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(reg.getCreatedOn() != null ? reg.getCreatedOn().format(DATE_FORMAT) : "");
                row.createCell(1).setCellValue(transportTypeRu(reg.getTransport_type()) != null ? transportTypeRu(reg.getTransport_type()) : "");
                row.createCell(2).setCellValue(reg.getPlate_no() != null ? reg.getPlate_no() : "");
                row.createCell(3).setCellValue(reg.getVin_code() != null ? reg.getVin_code() : "");
                row.createCell(4).setCellValue(reg.getCalculated_amount() != null ? reg.getCalculated_amount().doubleValue() : 0);

                row.createCell(5).setCellValue("");
                row.createCell(6).setCellValue(0);
                row.createCell(7).setCellValue(0);
                row.createCell(8).setCellValue(
                        reg.getCalculated_amount() != null ? reg.getCalculated_amount().doubleValue() : 0
                );
                continue;
            }

            // Есть услуги
            for (Services service : reg.getServices()) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(reg.getCreatedOn() != null ? reg.getCreatedOn().toString() : "");
                row.createCell(1).setCellValue(transportTypeRu(reg.getTransport_type()) != null ? transportTypeRu(reg.getTransport_type()) : "");
                row.createCell(2).setCellValue(reg.getPlate_no() != null ? reg.getPlate_no() : "");
                row.createCell(3).setCellValue(reg.getVin_code() != null ? reg.getVin_code() : "");
                row.createCell(4).setCellValue(reg.getCalculated_amount() != null ? reg.getCalculated_amount().doubleValue() : 0);

                row.createCell(5).setCellValue(service.getName_of_service() != null ? service.getName_of_service() : "");
                row.createCell(6).setCellValue(service.getPrice_for_service() != null ? service.getPrice_for_service().doubleValue() : 0);
                row.createCell(7).setCellValue(service.getAmount() != null ? service.getAmount().doubleValue() : 0);

                BigDecimal total = (reg.getCalculated_amount() != null ? reg.getCalculated_amount() : BigDecimal.ZERO)
                        .add(service.getPrice_for_service() != null ? service.getPrice_for_service() : BigDecimal.ZERO);

                row.createCell(8).setCellValue(total.doubleValue());
            }
        }

        // ===== AUTOSIZE =====
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // ===== WRITE =====
        workbook.write(outputStream);
    }


    private String transportTypeRu(String type) {
        if (type == null) {
            return "";
        }

        switch (type) {
            case "PASSENGER":
                return "Легковой автомобиль";
            case "SPECIAL":
                return "Спецтехника";
            case "CAR_CARRIER":
                return "Автовоз";
            case "FREIGHT":
                return "Грузовой АТС";
            case "COMPANY_CAR":
                return "Служебное авто";
            default:
                return type; // если вдруг прилетит что-то неизвестное
        }
    }

}
