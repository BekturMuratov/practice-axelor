package com.axelor.apps.seo.rest.dto;

public class MetaSelectDTO {
    private String value;
    private String title;

    public MetaSelectDTO() {}

    public MetaSelectDTO(String value, String title) {
        this.value = value;
        this.title = title;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}