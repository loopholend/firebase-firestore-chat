package com.example.chatapp.web.dto;

import jakarta.validation.constraints.NotBlank;

public class MessageForm {

    @NotBlank(message = "Message is required.")
    private String text;

    @NotBlank
    private String receiverId;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
}
