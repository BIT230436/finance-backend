package com.example.financebackend.dto;

import com.example.financebackend.entity.WalletShare;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class WalletShareDto {

    private Long id;
    private Long walletId;
    private String walletName;
    private Long sharedWithUserId;
    private String sharedWithUserEmail;
    private String sharedWithUserFullName;
    private WalletShare.Permission permission;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public Long getSharedWithUserId() {
        return sharedWithUserId;
    }

    public void setSharedWithUserId(Long sharedWithUserId) {
        this.sharedWithUserId = sharedWithUserId;
    }

    public String getSharedWithUserEmail() {
        return sharedWithUserEmail;
    }

    public void setSharedWithUserEmail(String sharedWithUserEmail) {
        this.sharedWithUserEmail = sharedWithUserEmail;
    }

    public String getSharedWithUserFullName() {
        return sharedWithUserFullName;
    }

    public void setSharedWithUserFullName(String sharedWithUserFullName) {
        this.sharedWithUserFullName = sharedWithUserFullName;
    }

    public WalletShare.Permission getPermission() {
        return permission;
    }

    public void setPermission(WalletShare.Permission permission) {
        this.permission = permission;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class CreateWalletShareRequest {
        @NotBlank(message = "Email người dùng được chia sẻ là bắt buộc")
        @Email(message = "Email không hợp lệ")
        private String sharedWithUserEmail;

        @NotNull(message = "Quyền truy cập là bắt buộc")
        @JsonDeserialize(using = WalletSharePermissionDeserializer.class)
        private WalletShare.Permission permission;

        public String getSharedWithUserEmail() {
            return sharedWithUserEmail;
        }

        public void setSharedWithUserEmail(String sharedWithUserEmail) {
            this.sharedWithUserEmail = sharedWithUserEmail != null ? sharedWithUserEmail.toLowerCase().trim() : null;
        }

        public WalletShare.Permission getPermission() {
            return permission;
        }

        public void setPermission(WalletShare.Permission permission) {
            this.permission = permission;
        }
    }
}

