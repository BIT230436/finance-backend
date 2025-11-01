package com.example.financebackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Email(message = "Email không hợp lệ")
    private String email; // Optional - if null, keep current email

    @Size(min = 2, message = "Họ tên phải có ít nhất 2 ký tự")
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String fullName; // Optional - if null, keep current name

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}

