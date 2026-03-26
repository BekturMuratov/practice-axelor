package com.axelor.apps.seo.rest.dto;

public class CustomsCheckpointDTO {
    private Long id;
    private String ccpName;
    private String direction;
    private String onlinePayments;
    private int priceForBooking;

    public CustomsCheckpointDTO() {}

    public CustomsCheckpointDTO(Long id, String ccpName, String direction, String onlinePayments, int priceForBooking) {
        this.id = id;
        this.ccpName = ccpName;
        this.direction = direction;
        this.onlinePayments = onlinePayments;
        this.priceForBooking = priceForBooking;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCcpName() {
        return ccpName;
    }

    public void setCcpName(String ccpName) {
        this.ccpName = ccpName;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getOnlinePayments() {
        return onlinePayments;
    }

    public void setOnlinePayments(String onlinePayments) {
        this.onlinePayments = onlinePayments;
    }

    public int getPriceForBooking() {
        return priceForBooking;
    }

    public void setPriceForBooking(int priceForBooking) {
        this.priceForBooking = priceForBooking;
    }
}