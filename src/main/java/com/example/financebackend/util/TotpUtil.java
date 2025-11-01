package com.example.financebackend.util;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Component;

@Component
public class TotpUtil {

    private final SecretGenerator secretGenerator;
    private final CodeGenerator codeGenerator;
    private final CodeVerifier codeVerifier;
    private final TimeProvider timeProvider;

    public TotpUtil() {
        this.secretGenerator = new DefaultSecretGenerator();
        this.codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
        this.timeProvider = new SystemTimeProvider();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    /**
     * Generate QR code URL for Google Authenticator
     * Format: otpauth://totp/{issuer}:{email}?secret={secret}&issuer={issuer}
     */
    public String generateQrCodeUrl(String email, String secret) {
        String issuer = "FinanceApp";
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", 
            issuer, email, secret, issuer);
    }
}
