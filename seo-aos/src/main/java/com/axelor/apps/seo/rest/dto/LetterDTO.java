package com.axelor.apps.seo.rest.dto;

public class LetterDTO {
    private Long id;
    private String fullName;
    private String email;
    private String messageContent;

    public LetterDTO(Long id, String fullName, String email, String messageContent) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.messageContent = messageContent;
    }

    public LetterDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
}
