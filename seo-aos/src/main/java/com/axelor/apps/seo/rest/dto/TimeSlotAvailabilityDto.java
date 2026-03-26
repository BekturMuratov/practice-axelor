package com.axelor.apps.seo.rest.dto;

public class TimeSlotAvailabilityDto {

    private String value;
    private String timeRange;     // 00:00-01:00
    private Integer freeSlots;    // количество свободных
    private String status;        // FREE / BOOKED / CLOSED

    public TimeSlotAvailabilityDto(String value,String timeRange, Integer freeSlots, String status) {
        this.value = value;
        this.timeRange = timeRange;
        this.freeSlots = freeSlots;
        this.status = status;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public Integer getFreeSlots() {
        return freeSlots;
    }

    public void setFreeSlots(Integer freeSlots) {
        this.freeSlots = freeSlots;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
