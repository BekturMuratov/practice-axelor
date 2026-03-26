package com.axelor.apps.seo.web;

import com.axelor.apps.seo.db.CustomsCheckpoint;
import com.axelor.apps.seo.db.repo.CustomsCheckpointRepository;
import com.axelor.apps.seo.service.CrudService;
import com.axelor.apps.seo.service.ReportService;
import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReportController {

    private final ReportService reportService;
    private final CustomsCheckpointRepository customsCheckpointRepository;
    private final CrudService crudService;

    @Inject
    public ReportController(ReportService reportService, CustomsCheckpointRepository customsCheckpointRepository, CrudService crudService){
        this.reportService = reportService;
        this.customsCheckpointRepository = customsCheckpointRepository;
        this.crudService = crudService;
    }

    public void getUserCcp(ActionRequest request, ActionResponse response) {
        User user = AuthUtils.getUser();
        response.setValue("customsCheckpoint", user.getCcp() == null ? Collections.emptySet() : Set.of(user.getCcp()));
    }

    private Map<String, Object> getDataForReport(ActionRequest request) {
        HashSet<CustomsCheckpoint> ccps = getCcpsFromRequest(request.getData().get("ccps"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate fromDate = LocalDate.parse((String) request.getData().get("fromDate"), formatter);
        LocalDate toDate = LocalDate.parse((String) request.getData().get("toDate"), formatter);

        Map<String, Object> result = new HashMap<>();
        result.put("fromDate", fromDate);
        result.put("toDate", toDate);
        result.put("ccps", ccps);
        return result;
    }

    private HashSet<CustomsCheckpoint> getCcpsFromRequest(Object ccpsCollection) {
        HashSet<CustomsCheckpoint> ccps = new HashSet<>();
        if (ccpsCollection instanceof Collection<?>) {
            ((Collection<?>) ccpsCollection).forEach(item -> {
                if (item instanceof LinkedHashMap) {
                    Long id = Long.valueOf(((LinkedHashMap<?, ?>) item).get("id").toString());
                    ccps.add(customsCheckpointRepository.find(id));
                }
            });
        }
        return ccps;
    }


    // Данные для отчета транзакций

    public void exportTransactionsOk(ActionRequest request, ActionResponse response){                                   // со статусом 250
        exportToCSV(request, response, "transactionsOk");
    }

    public void calculateTotalSumTransactions(ActionRequest request, ActionResponse response) {
        HashSet<CustomsCheckpoint> ccps = getCcpsFromRequest(request.getContext().get("customsCheckpoint"));
        LocalDate fromDate = LocalDate.parse(request.getContext().get("fromDateLocal").toString());
        LocalDate toDate = LocalDate.parse(request.getContext().get("toDateLocal").toString());
        if (ccps.isEmpty()) {
            ccps.add(AuthUtils.getUser().getCcp());
        }
        BigDecimal transactionsOkSum = reportService.calculateTotalSumTransactionsOk(ccps, fromDate, toDate);

        response.setValue("$totalSumsOkTransactions", transactionsOkSum);
    }

    public void getPagesInfoTransactions(ActionRequest request, ActionResponse response){
        HashSet<CustomsCheckpoint> ccps = getCcpsFromRequest(request.getContext().get("customsCheckpoint"));
        LocalDate fromDate = LocalDate.parse(request.getContext().get("fromDateLocal").toString());
        LocalDate toDate = LocalDate.parse(request.getContext().get("toDateLocal").toString());
        int amountDate = 40;
        if(request.getContext().get("amountDate") != null){
            amountDate = (int) request.getContext().get("amountDate");
        }
        if (ccps.isEmpty()) {
            ccps.add(AuthUtils.getUser().getCcp());
        }
        Map<String, Integer> stats = reportService.getTransactionsOkPages(ccps, fromDate, toDate, amountDate);
        int totalAmount = stats.get("totalGoods");
        int totalPages = stats.get("totalPages");
        response.setValue("$transactionOkPages", totalPages);
        response.setValue("$transactionOkAmount", totalAmount);
    }

    public void getTransactionsOk(ActionRequest request, ActionResponse response) {
        getReleasedData(request, response, "transactionsOk");
    }

    // Данные для отчета транзакций по номеру регистрации

    public void calculateTotalSumTransactionsRegistration(ActionRequest request, ActionResponse response) {
        String reqNumber = (String) request.getContext().get("reqNumber");

        BigDecimal transactionsOkSum = reportService.calculateTotalSumTransactionsRegistrationOk(reqNumber);

        response.setValue("$totalSumsOkTransactionsRegistration", transactionsOkSum);
    }

    public void getPagesInfoTransactionsRegistration(ActionRequest request, ActionResponse response){
        String reqNumber = (String) request.getContext().get("reqNumber");
        int amountDate = 40;
        if(request.getContext().get("amountDate") != null){
            amountDate = (int) request.getContext().get("amountDate");
        }

        Map<String, Integer> stats = reportService.getTransactionsRegistrationOkPages(reqNumber, amountDate);
        int totalAmount = stats.get("totalGoods");
        int totalPages = stats.get("totalPages");
        response.setValue("$transactionOkPages", totalPages);
        response.setValue("$transactionOkAmount", totalAmount);
    }

    public void getTransactionsRegistrationOk(ActionRequest request, ActionResponse response) {
        getReleasedDataVer2(request, response, "transactionsRegistrationOk");
    }

    public void exportTransactionsRegistrationOk(ActionRequest request, ActionResponse response){                                   // со статусом 250
        exportToCSVVer2(request, response, "transactionsRegistrationOk");
    }

    // Данные для общего использования

    private void scheduleFileDeletion(MetaFile metaFile) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            crudService.removeObject(metaFile);
            scheduler.shutdown();
        }, 20, TimeUnit.SECONDS);
    }

    public void getReleasedData(ActionRequest request, ActionResponse response, String dataType) {
        Map<String, Object> objectMap = getDataForReport(request);
        List<Map<String, Object>> data;
        int  pageSize = (int) request.getData().get("pageSize");
        int   page = (int) request.getData().get("page");

        switch (dataType) {
            case "transactionsOk":
                data = (List<Map<String, Object>>) reportService.getTransactionOk(
                        (HashSet<CustomsCheckpoint>) objectMap.get("ccps"),
                        (LocalDate) objectMap.get("fromDate"),
                        (LocalDate) objectMap.get("toDate"),
                        pageSize,
                        page,
                        false
                );
                break;
            default:
                throw new IllegalArgumentException("Invalid data type");
        }

        response.setData(data);
    }

    public void getReleasedDataVer2(ActionRequest request, ActionResponse response, String dataType) {
        String reqNumber = (String) request.getData().get("reqNumber");
        List<Map<String, Object>> data;
        int  pageSize = (int) request.getData().get("pageSize");
        int   page = (int) request.getData().get("page");

        switch (dataType) {
            case "transactionsRegistrationOk":
                data = (List<Map<String, Object>>) reportService.getTransactionRegistrationOk(
                        reqNumber,
                        pageSize,
                        page,
                        false
                );
                break;
            default:
                throw new IllegalArgumentException("Invalid data type");
        }

        response.setData(data);
    }

    private void exportToCSV(ActionRequest request, ActionResponse response, String typeExport) {
        HashSet<CustomsCheckpoint> ccps = getCcpsFromRequest(request.getContext().get("customsCheckpoint"));
        LocalDate fromDate = LocalDate.parse(request.getContext().get("fromDateLocal").toString());
        LocalDate toDate = LocalDate.parse(request.getContext().get("toDateLocal").toString());
        if (ccps.isEmpty()) {
            ccps.add(AuthUtils.getUser().getCcp());
        }
        try {
            MetaFile metaFile = null;
            if(typeExport.equals("transactionsOk")){
                metaFile = reportService.exportTransactionsOkToCsv(ccps, fromDate, toDate);
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

    private void exportToCSVVer2(ActionRequest request, ActionResponse response, String typeExport) {
        String reqNumber = (String) request.getContext().get("reqNumber");
        try {
            MetaFile metaFile = null;

            if(typeExport.equals("transactionsRegistrationOk")){
                metaFile = reportService.exportTransactionsRegistrationOkToCsv(reqNumber);
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
}