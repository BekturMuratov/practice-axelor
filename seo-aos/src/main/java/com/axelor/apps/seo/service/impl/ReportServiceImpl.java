package com.axelor.apps.seo.service.impl;

import com.axelor.apps.seo.db.*;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.apps.seo.service.ReportService;
import com.axelor.apps.seo.utils.StatusConstants;
import com.axelor.i18n.I18n;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.google.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import static org.jboss.resteasy.plugins.guice.i18n.LogMessages.LOGGER;

public class ReportServiceImpl implements ReportService {

    private static final String SUCCESS_STATUS = "success";

    @PersistenceContext
    private final EntityManager entityManager;
    private final CrudService crudService;

    @Inject
    public ReportServiceImpl(EntityManager entityManager, CrudService crudService) {
        this.entityManager = entityManager;
        this.crudService = crudService;
    }

    private List<Long> getCcpIds(HashSet<CustomsCheckpoint> ccps){
        return ccps.stream()
                .map(CustomsCheckpoint::getId)
                .collect(Collectors.toList());
    }

    // Методы для отчета транзакций
    public BigDecimal calculateTotalSumTransactionsOk(HashSet<CustomsCheckpoint> ccps, LocalDate fromDate, LocalDate toDate) {
        List<Long> ccpIds = getCcpIds(ccps);

        String sql = "SELECT COALESCE(SUM(payment.amount), 0) " +
                "FROM seo_payment payment " +
                "WHERE (payment.archived IS NULL OR payment.archived = false) " +
                "AND payment.payment_date BETWEEN :fromDate AND :toDate " +
                "AND payment.ccp IN :ccpIds " +
                "AND payment.status = :successStatus ";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        query.setParameter("ccpIds", ccpIds);
        query.setParameter("successStatus", SUCCESS_STATUS);

        BigDecimal totalSum = (BigDecimal) query.getSingleResult();
        return totalSum != null ? totalSum : BigDecimal.ZERO;
    }

    public Map<String, Integer> getTransactionsOkPages(HashSet<CustomsCheckpoint> ccps, LocalDate fromDate, LocalDate toDate, int pageSize) {
        List<Long> ccpIds = getCcpIds(ccps);

        String sql = "SELECT * " +
                "FROM seo_payment payment " +
                "WHERE (payment.archived IS NULL OR payment.archived = false) " +
                "AND payment.ccp IN :ccpIds " +
                "AND payment.payment_date BETWEEN :fromDate AND :toDate " +
                "AND payment.status = :successStatus ";

        Query query = entityManager.createNativeQuery(sql, Payment.class);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        query.setParameter("ccpIds", ccpIds);
        query.setParameter("successStatus", SUCCESS_STATUS);

        List<Payment> payments = query.getResultList();

        int totalGoods = payments.size();
        int totalPages = (int) Math.ceil((double) totalGoods / pageSize);

        Map<String, Integer> result = new HashMap<>();
        result.put("totalGoods", totalGoods);
        result.put("totalPages", totalPages > 0 ? totalPages : 1);

        return result;
    }

    public Object getTransactionOk(HashSet<CustomsCheckpoint> ccps, LocalDate fromDate, LocalDate toDate, int pageSize, int page, boolean isCsv) {
        List<Long> ccpIds = getCcpIds(ccps);

        String sql = "SELECT payment.payment_date, " +
                "       payment.operation_type, " +
                "       payment.amount, " +
                "       payment.requisite, " +
                "       ccp.ccp_name, " +
                "       payment.status, " +
                "       payment.payment_number, " +
                "       payment.operationid " +
                "FROM seo_payment payment " +
                "LEFT JOIN seo_customs_checkpoint ccp ON payment.ccp = ccp.id " +
                "WHERE (payment.archived IS NULL OR payment.archived = false) " +
                "AND payment.payment_date BETWEEN :fromDate AND :toDate " +
                "AND payment.ccp IN :ccpIds " +
                "AND payment.status = :successStatus " +
                "ORDER BY payment.payment_date DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        query.setParameter("ccpIds", ccpIds);
        query.setParameter("successStatus", SUCCESS_STATUS);

        List<Object[]> payments = query.getResultList();

        if (isCsv) {
            return payments;
        }

        return payments.stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .map(row -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("payment_date", row[0]);
                    data.put("operation_type", row[1]);
                    data.put("amount", row[2]);
                    data.put("requisite", row[3]);
                    data.put("ccp", row[4]);
                    data.put("status", I18n.get("Success"));
                    return data;
                })
                .collect(Collectors.toList());
    }

    public MetaFile exportTransactionsOkToCsv(HashSet<CustomsCheckpoint> ccps, LocalDate fromDate, LocalDate toDate) throws IOException {
        // Создание временного CSV файла
        File tempFile = MetaFiles.createTempFile("", ".csv").toFile();
        DecimalFormat decimalFormatter = new DecimalFormat("0.000");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        List<Object[]> payments = (List<Object[]>) getTransactionOk(ccps, fromDate, toDate, 1, 1, true);

        String[] headers = {
                "Дата платежа",
                "Тип операции",
                "Сумма",
                "Реквизит",
                "ПП",
                "Статус"
        };

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader(headers)
                .setRecordSeparator("\n")
                .build();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
            writer.write('\uFEFF');
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                for (Object[] payment : payments) {
                    List<Object> record = new ArrayList<>();
                    record.add(payment[0] != null
                            ? ((java.sql.Date) payment[0]).toLocalDate().format(dateFormatter)
                            : "");
                    record.add(getTranslateTypeOfOperation((String) payment[1])); // operationType
                    record.add(payment[2] != null ? decimalFormatter.format(payment[2]) : "");
                    record.add(payment[3]); // requisite
                    record.add(payment[4]); // ccp display field
                    record.add(I18n.get("Success")); // status

                    csvPrinter.printRecord(record);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write CSV file", e);
            throw new IOException("Failed to write to CSV file", e);
        }

        // Возврат MetaFile
        return getMetaTempFile(tempFile);
    }

    // Методы для отчета транзакций по номеру регистрации
    public BigDecimal calculateTotalSumTransactionsRegistrationOk(String reqNumber) {
        String sql = "SELECT COALESCE(SUM(payment.amount), 0) " +
                "FROM seo_payment payment " +
                "WHERE (payment.archived IS NULL OR payment.archived = false) " +
                "AND payment.status = :successStatus " +
                "AND payment.requisite = :reqNumber";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("reqNumber", reqNumber);
        query.setParameter("successStatus", SUCCESS_STATUS);

        BigDecimal totalSum = (BigDecimal) query.getSingleResult();
        return totalSum != null ? totalSum : BigDecimal.ZERO;
    }

    public Map<String, Integer> getTransactionsRegistrationOkPages(String reqNumber, int pageSize) {
        String sql = "SELECT * " +
                "FROM seo_payment payment " +
                "WHERE (payment.archived IS NULL OR payment.archived = false) " +
                "AND payment.status = :successStatus " +
                "AND payment.requisite = :reqNumber";

        Query query = entityManager.createNativeQuery(sql, Payment.class);
        query.setParameter("reqNumber", reqNumber);
        query.setParameter("successStatus", SUCCESS_STATUS);

        List<Payment> payments = query.getResultList();

        int totalGoods = payments.size();
        int totalPages = (int) Math.ceil((double) totalGoods / pageSize);

        Map<String, Integer> result = new HashMap<>();
        result.put("totalGoods", totalGoods);
        result.put("totalPages", totalPages > 0 ? totalPages : 1);

        return result;
    }

    public Object getTransactionRegistrationOk(String reqNumber, int pageSize, int page, boolean isCsv) {
        String sql = "SELECT payment.payment_date, " +
                "       payment.operation_type, " +
                "       payment.amount, " +
                "       payment.requisite, " +
                "       ccp.ccp_name, " +
                "       payment.status, " +
                "       payment.payment_number, " +
                "       payment.operationid " +
                "FROM seo_payment payment " +
                "LEFT JOIN seo_customs_checkpoint ccp ON payment.ccp = ccp.id " +
                "WHERE (payment.archived IS NULL OR payment.archived = false) " +
                "AND payment.status = :successStatus " +
                "AND payment.requisite = :reqNumber " +
                "ORDER BY payment.payment_date DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("reqNumber", reqNumber);
        query.setParameter("successStatus", SUCCESS_STATUS);

        List<Object[]> payments = query.getResultList();

        if (isCsv) {
            return payments;
        }

        return payments.stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .map(row -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("payment_date", row[0]);
                    data.put("operation_type", row[1]);
                    data.put("amount", row[2]);
                    data.put("requisite", row[3]);
                    data.put("ccp", row[4]);
                    data.put("status", I18n.get("Success"));
                    return data;
                })
                .collect(Collectors.toList());
    }

    public MetaFile exportTransactionsRegistrationOkToCsv(String reqNumber) throws IOException {
        // Создание временного CSV файла
        File tempFile = MetaFiles.createTempFile("", ".csv").toFile();
        DecimalFormat decimalFormatter = new DecimalFormat("0.000");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        List<Object[]> payments = (List<Object[]>) getTransactionRegistrationOk(reqNumber,1, 1, true);

        String[] headers = {"Дата платежа", "Сумма", "Реквизит", "Статус"};

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader(headers)
                .setRecordSeparator("\n")
                .build();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
            writer.write('\uFEFF');
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                for (Object[] payment : payments) {
                    List<Object> record = new ArrayList<>();
                    record.add(payment[0] != null
                            ? ((java.sql.Date) payment[0]).toLocalDate().format(dateFormatter)
                            : "");
                    record.add(payment[2] != null ? decimalFormatter.format(payment[2]) : ""); // sum
                    record.add(payment[3]); // requisite
                    record.add(I18n.get("Success")); // status

                    csvPrinter.printRecord(record);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write CSV file", e);
            throw new IOException("Failed to write to CSV file", e);
        }

        // Возврат MetaFile
        return getMetaTempFile(tempFile);
    }

    private String getTranslateTypeOfOperation(String status){
        switch (status){
            case StatusConstants.PAYMENT_OPERATION_BANK_NOTIFICATION:
                return I18n.get("Payment notification from the bank");
            case StatusConstants.PAYMENT_OPERATION_QR_REQUEST:
                return I18n.get("Request to create a QR code");
            default:
                return status;
        }
    }

    // Метод для создания файла
    private MetaFile getMetaTempFile(File tempFile) throws FileNotFoundException {
        if (tempFile.exists()) {
            MetaFile metaFile = new MetaFile();
            metaFile.setFileSize(tempFile.length());
            metaFile.setFileName(tempFile.getName());
            metaFile.setFileType("csv");
            metaFile.setFilePath(tempFile.getAbsolutePath());
            MetaFile metaFile1 = crudService.persistObject(metaFile);
            return metaFile1;
        } else {
            throw new FileNotFoundException("Temp file does not exist: " + tempFile.getPath());
        }
    }

}
