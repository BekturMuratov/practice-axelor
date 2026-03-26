package com.axelor.apps.seo.service.impl;

import com.axelor.apps.seo.db.CustomsCheckpoint;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.apps.seo.service.DashboardService;
import com.axelor.apps.seo.utils.StatusConstants;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaFileRepository;
import com.google.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardServiceImpl implements DashboardService {
    @PersistenceContext
    private final EntityManager entityManager;
    private final CrudService crudService;
    private final MetaFileRepository metaFileRepository;

    @Inject
    public DashboardServiceImpl(EntityManager entityManager, CrudService crudService, MetaFileRepository metaFileRepository) {
        this.entityManager = entityManager;
        this.crudService = crudService;
        this.metaFileRepository = metaFileRepository;
    }

    @Override
    public MetaFile exportChartToCSV(LocalDate fromDate, LocalDate toDate, String typeOfChart, CustomsCheckpoint ccp, boolean needFullInfo) throws IOException {
        File tempFile = MetaFiles.createTempFile("", ".csv").toFile();
        DecimalFormat decimalFormatter =  new DecimalFormat("0.000");
        List<Object[]> data;
        String[] headers;

        DateTimeFormatter dateTimeFormatterForMonths = DateTimeFormatter.ofPattern("MM-yyyy");
        DateTimeFormatter dateTimeFormatterForDays = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        switch (typeOfChart) {
            case StatusConstants.CHART_FINANCIAL_INDICATORS_BY_MONTH:
                headers = new String[]{"Дата", "Общая сумма"};
                data = getFinancialIndicatorsDataByMonth(fromDate, toDate, ccp.getId(), needFullInfo);
                break;
            case StatusConstants.CHART_REGISTRATION_NUMBER_BY_MONTH:
                headers = new String[]{"Дата", "Количество"};
                data = getRegistrationNumberDataByMonth(fromDate, toDate, ccp.getId(), needFullInfo);
                break;
            case StatusConstants.CHART_FINANCIAL_INDICATORS_BY_DAY:
                headers = new String[]{"Дата", "Общая сумма"};
                data = getFinancialIndicatorsDataByDay(fromDate, toDate, ccp.getId(), needFullInfo);
                break;
            case StatusConstants.CHART_REGISTRATION_NUMBER_BY_DAY:
                headers = new String[]{"Дата", "Количество"};
                data = getRegistrationNumberDataByDay(fromDate, toDate, ccp.getId(), needFullInfo);
                break;
            default:
                throw new IOException("Unsupported chart type: " + typeOfChart);
        }

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader(headers)
                .setRecordSeparator("\n")
                .build();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
            writer.write('\uFEFF');
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

                for (Object[] ccpData : data) {
                    String formattedDate = null;
                    String formattedValue = null;

                    switch (typeOfChart) {
                        case StatusConstants.CHART_FINANCIAL_INDICATORS_BY_DAY:
                        case StatusConstants.CHART_REGISTRATION_NUMBER_BY_DAY:
                            if (ccpData[0] instanceof java.sql.Date) {
                                formattedDate = ((java.sql.Date) ccpData[0]).toLocalDate().format(dateTimeFormatterForDays);
                            } else {
                                formattedDate = ccpData[0].toString();
                            }
                            formattedValue = ccpData[1].toString();
                            break;

                        case StatusConstants.CHART_FINANCIAL_INDICATORS_BY_MONTH:
                        case StatusConstants.CHART_REGISTRATION_NUMBER_BY_MONTH:
                            if (ccpData[0] instanceof java.sql.Date) {
                                formattedDate = ((java.sql.Date) ccpData[0]).toLocalDate().format(dateTimeFormatterForMonths);
                            } else {
                                formattedDate = ccpData[0].toString();
                            }
                            formattedValue = ccpData[1].toString();
                            break;

                        default:
                            throw new IOException("Unsupported chart type: " + typeOfChart);
                    }

                    csvPrinter.printRecord(formattedDate, formattedValue);
                }
            }
        } catch (IOException e) {
            throw new IOException("Failed to write to CSV file", e);
        }

        if (tempFile.exists()) {
            MetaFile metaFile = new MetaFile();
            metaFile.setFileSize(tempFile.length());
            metaFile.setFileName(tempFile.getName());
            metaFile.setFileType("csv");
            metaFile.setFilePath(tempFile.getAbsolutePath());
            return crudService.persistObject(metaFile);
        } else {
            throw new FileNotFoundException("Temp file does not exist: " + tempFile.getPath());
        }
    }

    public List<Object[]> getFinancialIndicatorsDataByMonth(LocalDate fromDate, LocalDate toDate, Long ccpId, boolean needFullInfo) {
        String sql = "WITH RECURSIVE date_range AS ( " +
                "    SELECT DATE(:fromDate) AS date " +
                "    UNION ALL " +
                "    SELECT DATE(date + INTERVAL '1 month') " +
                "    FROM date_range " +
                "    WHERE date + INTERVAL '1 month' <= DATE(:toDate) " +
                ") " +
                "SELECT " +
                "    dates.date AS release_month, " +
                "    ROUND(COALESCE(SUM(ccp.price_for_booking), 0) / 1000.0, 3) AS total_sum " +
                "FROM date_range dates " +
                "LEFT JOIN seo_registration r " +
                "    ON dates.date = DATE_TRUNC('month', r.created_on) " +
                "    AND (r.archived IS NULL OR r.archived = false) " +
                "    AND DATE(r.created_on) BETWEEN DATE(:fromDate) AND DATE(:toDate) " +
                (needFullInfo ? "" : " AND r.ccp = :ccp ") +  // Условие для фильтрации по ccp, если needFullInfo = false
                "LEFT JOIN seo_customs_checkpoint ccp ON ccp.id = r.ccp " +
                "GROUP BY dates.date " +
                "ORDER BY dates.date";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);

        // Устанавливаем параметр ccp только если needFullInfo = false
        if (!needFullInfo) {
            query.setParameter("ccp", ccpId);
        }

        return query.getResultList();
    }


    public List<Object[]> getRegistrationNumberDataByMonth(LocalDate fromDate, LocalDate toDate,  Long ccpId, boolean needFullInfo) {
        String sql = "WITH RECURSIVE date_range AS ( " +
                "    SELECT DATE(:fromDate) AS date " +
                "    UNION ALL " +
                "    SELECT DATE(date + INTERVAL '1 month') " +
                "    FROM date_range " +
                "    WHERE date + INTERVAL '1 month' <= DATE(:toDate) " +
                ") " +
                " SELECT " +
                "    dates.date AS release_date," +
                "    COALESCE(COUNT(r.id), 0) AS released_count " +
                " FROM date_range dates " +
                " LEFT JOIN" +
                "    seo_registration r" +
                "    ON dates.date = DATE_TRUNC('month', r.created_on) " +
                "    AND (archived IS NULL OR archived = false) " +
                (needFullInfo ? "" : " AND r.ccp = :ccp ") +  // Условие для фильтрации по ccp, если needFullInfo = false
                "GROUP BY dates.date " +
                "ORDER BY dates.date ";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        if (!needFullInfo) {
            query.setParameter("ccp", ccpId);
        }

        return query.getResultList();
    }

    public List<Object[]> getFinancialIndicatorsDataByDay(LocalDate fromDate, LocalDate toDate,  Long ccpId, boolean needFullInfo) {
        String sql =
                "WITH RECURSIVE date_range AS ( " +
                        "    SELECT DATE(:fromDate) AS date " +
                        "    UNION ALL " +
                        "    SELECT DATE(date + INTERVAL '1 day') " +
                        "    FROM date_range " +
                        "    WHERE date + INTERVAL '1 day' <= DATE(:toDate) " +
                        ") " +
                        "SELECT " +
                        "    dates.date AS release_day, " +
                        "    ROUND(COALESCE(SUM(ccp.price_for_booking), 0) / 1000.0, 3) AS total_sum " +
                        "FROM date_range dates " +
                        "LEFT JOIN seo_registration r " +
                        "    ON dates.date = DATE_TRUNC('day', r.created_on) " +
                        "    AND (r.archived IS NULL OR r.archived = false) " +
                        "    AND DATE(r.created_on) BETWEEN DATE(:fromDate) AND DATE(:toDate) " +
                        (needFullInfo ? "" : " AND r.ccp = :ccp ") +  // Условие для фильтрации по ccp, если needFullInfo = false
                        "LEFT JOIN seo_customs_checkpoint ccp ON ccp.id = r.ccp " +
                        "GROUP BY dates.date " +
                        "ORDER BY dates.date";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        if (!needFullInfo) {
            query.setParameter("ccp", ccpId);
        }
        return query.getResultList();
    }

    public List<Object[]> getRegistrationNumberDataByDay(LocalDate fromDate, LocalDate toDate, Long ccpId, boolean needFullInfo) {
        String sql = "WITH RECURSIVE date_range AS ( " +
                "    SELECT DATE(:fromDate) AS date " +
                "    UNION ALL " +
                "    SELECT DATE(date + INTERVAL '1 day') " +
                "    FROM date_range " +
                "    WHERE date + INTERVAL '1 day' <= DATE(:toDate) " +
                ") " +
                " SELECT " +
                "    dates.date AS release_date," +
                "    COALESCE(COUNT(r.id), 0) AS released_count " +
                " FROM date_range dates " +
                " LEFT JOIN" +
                "    seo_registration r" +
                "    ON dates.date = DATE_TRUNC('day', r.created_on) " +
                "    AND (archived IS NULL OR archived = false) " +
                (needFullInfo ? "" : " AND r.ccp = :ccp ") +  // Условие для фильтрации по ccp, если needFullInfo = false
                "GROUP BY dates.date " +
                "ORDER BY dates.date ";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        if (!needFullInfo) {
            query.setParameter("ccp", ccpId);
        }
        return query.getResultList();
    }

}
