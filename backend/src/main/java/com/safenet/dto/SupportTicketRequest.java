package com.safenet.dto;

public class SupportTicketRequest {
    private String category;
    private String subject;
    private String details;

    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }
    public String getSubject() { return subject; }
    public void setSubject(String v) { subject = v; }
    public String getDetails() { return details; }
    public void setDetails(String v) { details = v; }
}
