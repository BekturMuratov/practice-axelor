package com.axelor.apps.seo.web;

import com.axelor.apps.seo.db.CustomsCheckpoint;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.apps.seo.service.DashboardService;
import com.axelor.apps.seo.utils.StatusConstants;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DashboardController {

    private final DashboardService dashboardService;
    private final CrudService crudService;

    @Inject
    public DashboardController(DashboardService dashboardService, CrudService crudService) {
        this.dashboardService = dashboardService;
        this.crudService = crudService;
    }

    public void exportFinancialIndicatorsByMonth(ActionRequest request, ActionResponse response) {
        String typeOfChart = StatusConstants.CHART_FINANCIAL_INDICATORS_BY_MONTH;
        String fromDateString = (String) request.getContext().get("fromDateForMonthlyIndicators");
        String toDateString = (String) request.getContext().get("toDateForMonthlyIndicators");
        exportToCSV(request, response, true, typeOfChart, fromDateString, toDateString);
    }

    public void exportFinancialIndicatorsByDay(ActionRequest request, ActionResponse response) {
        String typeOfChart = StatusConstants.CHART_FINANCIAL_INDICATORS_BY_DAY;
        String fromDateString = (String) request.getContext().get("fromDateForDailyIndicators");
        String toDateString = (String) request.getContext().get("toDateForDailyIndicators");
        exportToCSV(request, response, true, typeOfChart, fromDateString, toDateString);
    }

    public void exportRegistrationNumberByMonth(ActionRequest request, ActionResponse response) {
        String typeOfChart = StatusConstants.CHART_REGISTRATION_NUMBER_BY_MONTH;
        String fromDateString = (String) request.getContext().get("fromDateForMonthlyIndicators");
        String toDateString = (String) request.getContext().get("toDateForMonthlyIndicators");
        exportToCSV(request, response, true, typeOfChart, fromDateString, toDateString);
    }

    public void exportRegistrationNumberByDay(ActionRequest request, ActionResponse response) {
        String typeOfChart = StatusConstants.CHART_REGISTRATION_NUMBER_BY_DAY;
        String fromDateString = (String) request.getContext().get("fromDateForDailyIndicators");
        String toDateString = (String) request.getContext().get("toDateForDailyIndicators");
        exportToCSV(request, response, true, typeOfChart, fromDateString, toDateString);
    }

    private void exportToCSV(ActionRequest request, ActionResponse response, boolean needMoreInfo, String typeOfChart,
                             String fromDateString, String toDateString) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate fromDate = LocalDate.parse(fromDateString, formatter);
        LocalDate toDate = LocalDate.parse(toDateString, formatter);

        boolean needFullInfo = false;
        CustomsCheckpoint customsCheckpoint = null;
        if (needMoreInfo) {
            Map<String, Object> customsCheckpointMap = (Map<String, Object>) request.getContext().get("customsCheckpoint");
            ObjectMapper objectMapper = new ObjectMapper();
            customsCheckpoint = objectMapper.convertValue(customsCheckpointMap, CustomsCheckpoint.class);
            needFullInfo = (boolean) request.getContext().get("needFullInfo");
        }

        try {
            MetaFile metaFile;
            if (needMoreInfo) {
                metaFile = dashboardService.exportChartToCSV(fromDate, toDate, typeOfChart, customsCheckpoint, needFullInfo);
            } else {
                metaFile = dashboardService.exportChartToCSV(fromDate, toDate, typeOfChart, null, false);
            }

            if (metaFile != null) {
                response.setView(ActionView.define("download ccp data")
                        .add("html", "ws/rest/" + MetaFile.class.getCanonicalName() + "/" + metaFile.getId() + "/content/download")
                        .param("download", "dtrue")
                        .map());

                scheduleFileDeletion(metaFile);
            } else {
                response.setError("No file was generated for export.");
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void exportToCSV(ActionRequest request, ActionResponse response, boolean needMoreInfo, String typeOfChart) {
        String fromDateString = (String) request.getContext().get("fromDateLocal");
        String toDateString = (String) request.getContext().get("toDateLocal");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate fromDate = LocalDate.parse(fromDateString, formatter);
        LocalDate toDate = LocalDate.parse(toDateString, formatter);

        boolean needFullInfo = false;
        CustomsCheckpoint customsCheckpoint = null;
        if (needMoreInfo) {
            Map<String, Object> customsCheckpointMap = (Map<String, Object>) request.getContext().get("customsCheckpoint");
            ObjectMapper objectMapper = new ObjectMapper();
            customsCheckpoint = objectMapper.convertValue(customsCheckpointMap, CustomsCheckpoint.class);
            needFullInfo = (boolean) request.getContext().get("needFullInfo");
        }

        try {
            MetaFile metaFile;
            if (needMoreInfo) {
                metaFile = dashboardService.exportChartToCSV(fromDate, toDate, typeOfChart, customsCheckpoint, needFullInfo);
            } else {
                metaFile = dashboardService.exportChartToCSV(fromDate, toDate, typeOfChart, null, false);
            }

            if (metaFile != null) {
                response.setView(ActionView.define("download ccp data")
                        .add("html", "ws/rest/" + MetaFile.class.getCanonicalName() + "/" + metaFile.getId() + "/content/download")
                        .param("download", "true")
                        .map());

                scheduleFileDeletion(metaFile);
            } else {
                response.setError("No file was generated for export.");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void scheduleFileDeletion(MetaFile metaFile) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            crudService.removeObject(metaFile);
            scheduler.shutdown();
        }, 20, TimeUnit.SECONDS);
    }
}

