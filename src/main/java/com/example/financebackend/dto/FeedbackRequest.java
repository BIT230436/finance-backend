package com.example.financebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FeedbackRequest {

    @NotBlank(message = "Tiêu đề là bắt buộc")
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự")
    private String subject;

    @NotBlank(message = "Nội dung là bắt buộc")
    @Size(max = 5000, message = "Nội dung không được vượt quá 5000 ký tự")
    private String message;
    
    // Alias for "content" field from frontend
    private String content;

    private String type = "FEEDBACK"; // "FEEDBACK" or "BUG_REPORT"

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
        // Auto-map content to message if message is null
        if (this.message == null && content != null) {
            this.message = content;
        }
    }
    
    // Get message or content (prioritize message)
    public String getMessageOrContent() {
        return message != null ? message : content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

