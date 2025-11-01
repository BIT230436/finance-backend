package com.example.financebackend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budgets")
public class Budget {

    public enum Period { MONTHLY, WEEKLY, CUSTOM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Period period = Period.MONTHLY;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal usedAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal alertThreshold = new BigDecimal("0.80");
    
    @Column(nullable = false)
    private Boolean alertSent50 = false;  // Alert at 50%
    
    @Column(nullable = false)
    private Boolean alertSent80 = false;  // Alert at 80%
    
    @Column(nullable = false)
    private Boolean alertSent95 = false;  // Alert at 95%
    
    @Column(nullable = false)
    private Boolean alertSent100 = false; // Alert at 100%+

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Period getPeriod() { return period; }
    public void setPeriod(Period period) { this.period = period; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }

    public BigDecimal getUsedAmount() { return usedAmount; }
    public void setUsedAmount(BigDecimal usedAmount) { this.usedAmount = usedAmount; }

    public BigDecimal getAlertThreshold() { return alertThreshold; }
    public void setAlertThreshold(BigDecimal alertThreshold) { this.alertThreshold = alertThreshold; }
    
    public Boolean getAlertSent50() { return alertSent50; }
    public void setAlertSent50(Boolean alertSent50) { this.alertSent50 = alertSent50; }
    
    public Boolean getAlertSent80() { return alertSent80; }
    public void setAlertSent80(Boolean alertSent80) { this.alertSent80 = alertSent80; }
    
    public Boolean getAlertSent95() { return alertSent95; }
    public void setAlertSent95(Boolean alertSent95) { this.alertSent95 = alertSent95; }
    
    public Boolean getAlertSent100() { return alertSent100; }
    public void setAlertSent100(Boolean alertSent100) { this.alertSent100 = alertSent100; }
}
