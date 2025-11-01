package com.example.financebackend.dto;

import com.example.financebackend.entity.WalletShare;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Custom deserializer for WalletShare.Permission enum
 * Maps frontend values (READ_ONLY) to backend enum values (VIEWER)
 */
public class WalletSharePermissionDeserializer extends JsonDeserializer<WalletShare.Permission> {

    @Override
    public WalletShare.Permission deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        
        if (value == null) {
            return WalletShare.Permission.VIEWER;
        }
        
        // Map frontend values to backend enum values
        switch (value.toUpperCase()) {
            case "READ_ONLY":
            case "VIEWER":
                return WalletShare.Permission.VIEWER;
            case "EDITOR":
            case "EDIT":
                return WalletShare.Permission.EDITOR;
            case "OWNER":
                return WalletShare.Permission.OWNER;
            default:
                // Try to parse as enum name directly
                try {
                    return WalletShare.Permission.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Default to VIEWER if unknown value
                    return WalletShare.Permission.VIEWER;
                }
        }
    }
}

