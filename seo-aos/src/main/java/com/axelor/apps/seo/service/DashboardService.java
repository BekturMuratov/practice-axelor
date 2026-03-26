package com.axelor.apps.seo.service;

import com.axelor.apps.seo.db.CustomsCheckpoint;
import com.axelor.meta.db.MetaFile;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    MetaFile exportChartToCSV(LocalDate fromDate, LocalDate toDate, String typeOfChart, CustomsCheckpoint ccp, boolean needFullInfo) throws IOException;
    List<Object[]> getFinancialIndicatorsDataByMonth(LocalDate fromDate, LocalDate toDate, Long ccpId,  boolean needFullInfo);
    List<Object[]> getRegistrationNumberDataByMonth(LocalDate fromDate, LocalDate toDate,Long ccpId,  boolean needFullInfo);

}