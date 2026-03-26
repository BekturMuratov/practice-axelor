package com.axelor.apps.seo.integrations.bakai.dto;

public class GenerateQrRequestDto {
    private String accountNo;
    private Integer currencyId;
    private double amount;
    private String operationID;
    private String qrTtlUnits;
    private int qrTtl;
    private String prefix;

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getOperationID() {
        return operationID;
    }

    public void setOperationID(String operationID) {
        this.operationID = operationID;
    }

    public String getQrTtlUnits() {
        return qrTtlUnits;
    }

    public void setQrTtlUnits(String qrTtlUnits) {
        this.qrTtlUnits = qrTtlUnits;
    }

    public int getQrTtl() {
        return qrTtl;
    }

    public void setQrTtl(int qrTtl) {
        this.qrTtl = qrTtl;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}