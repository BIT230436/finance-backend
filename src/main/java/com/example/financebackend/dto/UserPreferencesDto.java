package com.example.financebackend.dto;

public class UserPreferencesDto {

    private String defaultCurrency;
    private String currencyFormat; // "dot" or "comma"
    private String dateFormat; // "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd"
    private String language; // "vi" or "en"

    public UserPreferencesDto() {
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public String getCurrencyFormat() {
        return currencyFormat;
    }

    public void setCurrencyFormat(String currencyFormat) {
        this.currencyFormat = currencyFormat;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}

