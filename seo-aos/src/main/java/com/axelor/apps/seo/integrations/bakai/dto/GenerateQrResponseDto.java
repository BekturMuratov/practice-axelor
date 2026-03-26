package com.axelor.apps.seo.integrations.bakai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenerateQrResponseDto {
    private String qrImage;
    private String qrLink;
    private String qrImageWithFrame;

    public String getQrImage() {
        return qrImage;
    }

    public void setQrImage(String qrImage) {
        this.qrImage = qrImage;
    }

    public String getQrLink() {
        return qrLink;
    }

    public void setQrLink(String qrLink) {
        this.qrLink = qrLink;
    }

    public String getQrImageWithFrame() {
        return qrImageWithFrame;
    }

    public void setQrImageWithFrame(String qrImageWithFrame) {
        this.qrImageWithFrame = qrImageWithFrame;
    }
}