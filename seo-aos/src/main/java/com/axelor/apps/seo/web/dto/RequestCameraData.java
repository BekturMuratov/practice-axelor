package com.axelor.apps.seo.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonRootName(value = "Camera")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestCameraData {
    @JsonProperty("crossRecordSyscode")
    private String crossRecordSyscode;

    @JsonProperty("cameraIndexCode")
    private String cameraIndexCode;

    @JsonProperty("plateNo")
    private String plateNo;

    @JsonProperty("ownerName")
    private String ownerName;

    @JsonProperty("contact")
    private String contact;

    @JsonProperty("crossTime")
    private String crossTime;

    @JsonProperty("vehicleColor ")
    private Integer vehicleColor;

    @JsonProperty("vehicleType ")
    private Integer vehicleType;

    @JsonProperty("country")
    private Integer country;

    @JsonProperty("vehicleDirectionType")
    private Integer vehicleDirectionType;

    @JsonProperty("vehiclePicPath")
    private String vehiclePicPath;

    public RequestCameraData() {
    }

    public RequestCameraData(String crossRecordSyscode, String cameraIndexCode, String plateNo, String ownerName, String contact,  String crossTime, Integer vehicleColor, Integer vehicleType, Integer country, Integer vehicleDirectionType, String vehiclePicPath) {
        this.crossRecordSyscode = crossRecordSyscode;
        this.cameraIndexCode = cameraIndexCode;
        this.plateNo = plateNo;
        this.ownerName = ownerName;
        this.contact = contact;
        this.crossTime = crossTime;
        this.vehicleColor = vehicleColor;
        this.vehicleType = vehicleType;
        this.country = country;
        this.vehicleDirectionType = vehicleDirectionType;
        this.vehiclePicPath = vehiclePicPath;
    }

    public String getCrossRecordSyscode() {
        return crossRecordSyscode;
    }

    public void setCrossRecordSyscode(String crossRecordSyscode) {
        this.crossRecordSyscode = crossRecordSyscode;
    }

    public String getCameraIndexCode() {
        return cameraIndexCode;
    }

    public void setCameraIndexCode(String cameraIndexCode) {
        this.cameraIndexCode = cameraIndexCode;
    }

    public String getPlateNo() {
        return plateNo;
    }

    public void setPlateNo(String plateNo) {
        this.plateNo = plateNo;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getCrossTime() {
        return crossTime;
    }

    public void setCrossTime(String crossTime) {
        this.crossTime = crossTime;
    }

    public Integer getVehicleColor() {
        return vehicleColor;
    }

    public void setVehicleColor(Integer vehicleColor) {
        this.vehicleColor = vehicleColor;
    }

    public Integer getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(Integer vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Integer getCountry() {
        return country;
    }

    public void setCountry(Integer country) {
        this.country = country;
    }

    public Integer getVehicleDirectionType() {
        return vehicleDirectionType;
    }

    public void setVehicleDirectionType(Integer vehicleDirectionType) {
        this.vehicleDirectionType = vehicleDirectionType;
    }

    public String getVehiclePicPath() {
        return vehiclePicPath;
    }

    public void setVehiclePicPath(String vehiclePicPath) {
        this.vehiclePicPath = vehiclePicPath;
    }
}

