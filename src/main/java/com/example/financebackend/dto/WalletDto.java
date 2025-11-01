package com.example.financebackend.dto;

import com.example.financebackend.entity.Wallet.WalletType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class WalletDto {

    private Long id;

    @NotBlank(message = "Tên ví là bắt buộc")
    @Size(max = 100, message = "Tên ví không được vượt quá 100 ký tự")
    private String name;

    @NotNull(message = "Loại ví là bắt buộc")
    private WalletType type;

    @NotBlank(message = "Loại tiền tệ là bắt buộc")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Loại tiền tệ phải là mã ISO 4217 (3 chữ cái)")
    private String currency;

    private BigDecimal balance;

    private Boolean isDefault;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public WalletType getType() { return type; }
    public void setType(WalletType type) { this.type = type; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
}
