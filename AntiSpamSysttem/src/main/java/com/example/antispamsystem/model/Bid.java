package com.example.antispamsystem.model;

import java.time.LocalDateTime;

public class Bid {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private String status;
    private String presentationLink;
    private String comments;
    private LocalDateTime createdDate;
    private String ip; // скрытое поле IP
    private String formattedDate; // отформатированная дата для отображения

    public Bid() {}

    public Bid(Long id, String name, String phone, String email, String status, String presentationLink, String comments, LocalDateTime createdDate, String ip) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.presentationLink = presentationLink;
        this.comments = comments;
        this.createdDate = createdDate;
        this.ip = ip;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPresentationLink() { return presentationLink; }
    public void setPresentationLink(String presentationLink) { this.presentationLink = presentationLink; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
}
