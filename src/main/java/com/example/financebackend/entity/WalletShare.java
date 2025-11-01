package com.example.financebackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_shares", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"wallet_id", "shared_with_user_id"})
})
public class WalletShare {

    public enum Permission {
        OWNER,   // Chủ sở hữu (có toàn quyền)
        EDITOR,  // Chỉnh sửa (có thể thêm/sửa/xóa giao dịch)
        VIEWER   // Chỉ xem (chỉ đọc)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_with_user_id")
    private User sharedWithUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Permission permission = Permission.VIEWER;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public User getSharedWithUser() {
        return sharedWithUser;
    }

    public void setSharedWithUser(User sharedWithUser) {
        this.sharedWithUser = sharedWithUser;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

