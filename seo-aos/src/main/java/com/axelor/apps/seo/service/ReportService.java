package com.axelor.apps.seo.service;

import com.axelor.apps.seo.db.CustomsCheckpoint;
import com.axelor.meta.db.MetaFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;

public interface ReportService {

    // Интерфейс для отчета транзакций
    BigDecimal calculateTotalSumTransactionsOk(HashSet<CustomsCheckpoint> ccps, LocalDate fromDate, LocalDate toDate);
    Map<String, Integer> getTransactionsOkPages(HashSet<CustomsCheckpoint> ccps, LocalDate fromDate, LocalDate toDate, int pageSize);
    Object getTransactionOk(HashSet<CustomsCheckpoint> ccps, LocalDate fromDate, LocalDate toDate,  int pageSize, int page,  boolean isCsv);
    MetaFile exportTransactionsOkToCsv(HashSet<CustomsCheckpoint> ccps, LocalDate fromDate, LocalDate toDate) throws IOException;


    // Интерфейс для отчета транзакций по номеру регистрации
    BigDecimal calculateTotalSumTransactionsRegistrationOk(String reqNumber);
    Map<String, Integer> getTransactionsRegistrationOkPages(String reqNumber, int pageSize);
    Object getTransactionRegistrationOk(String reqNumber, int pageSize, int page,  boolean isCsv);
    MetaFile exportTransactionsRegistrationOkToCsv(String reqNumber) throws IOException;
}
