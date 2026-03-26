package com.axelor.apps.svh.service.impl;

import com.axelor.apps.svh.db.Registration;
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

import static com.axelor.apps.svh.utils.StatusConstants.*;

public class RegistrationServiceImpl implements RegistrationService {

    @Inject
    RegistrationRepository registrationRepository;



    @Override
    public BigDecimal getAllTotalOfCars(LocalDate from, LocalDate to) {

        Registration registration = new Registration();

        if(from == null || to == null) {
            throw new IllegalArgumentException("Date range cannot be null");
        }

        if (Boolean.TRUE.equals(registration.getReleased())) {
            throw new RuntimeException("Registration already released");
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

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void exportRegistrationsToExcel(LocalDate from, LocalDate to, OutputStream outputStream) throws Exception {

        EntityManager em = com.axelor.db.JPA.em();

        List<Registration> registrations = em.createQuery(
                        "SELECT r FROM Registration r " +
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
                "Дата регистрации",
                "Тип АТС",
                "Гос номер",
                "VIN код",
                "Стоимость хранения"
        };

        for (int i = 0; i < columns.length; i++) {
            header.createCell(i).setCellValue(columns[i]);
        }

        int rowIdx = 1;

        // ===== DATA =====
        for (Registration reg : registrations) {

            Row row = sheet.createRow(rowIdx++);

            row.createCell(0).setCellValue(
                    reg.getCreatedOn() != null ? reg.getCreatedOn().format(DATE_FORMAT) : ""
            );

            row.createCell(1).setCellValue(
                    transportTypeRu(reg.getTransport_type()) != null ?
                            transportTypeRu(reg.getTransport_type()) : ""
            );

            row.createCell(2).setCellValue(
                    reg.getPlate_no() != null ? reg.getPlate_no() : ""
            );

            row.createCell(3).setCellValue(
                    reg.getVin_code() != null ? reg.getVin_code() : ""
            );

            row.createCell(4).setCellValue(
                    reg.getCalculated_amount() != null ?
                            reg.getCalculated_amount().doubleValue() : 0
            );
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
            case PASSENGER:
                return "Легковой автомобиль";
            case SPECIAL:
                return "Спецтехника";
            case CAR_CARRIER:
                return "Автовоз";
            case TRUCK:
                return "Грузовой АТС";
            case COMPANY_CAR:
                return "Служебное авто";
            default:
                return type;
        }
    }

}
