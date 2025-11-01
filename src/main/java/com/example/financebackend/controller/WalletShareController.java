package com.example.financebackend.controller;

import com.example.financebackend.dto.WalletShareDto;
import com.example.financebackend.dto.WalletSharePermissionDeserializer;
import com.example.financebackend.entity.WalletShare;
import com.example.financebackend.service.WalletShareService;
import com.example.financebackend.util.AuthUtil;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet-shares")
public class WalletShareController {

    private final WalletShareService walletShareService;

    public WalletShareController(WalletShareService walletShareService) {
        this.walletShareService = walletShareService;
    }

    @GetMapping("/shared-with-me")
    public List<WalletShareDto> getSharedWallets() {
        Long userId = AuthUtil.getCurrentUserId();
        return walletShareService.getSharedWallets(userId);
    }

    @GetMapping("/wallet/{walletId}")
    public List<WalletShareDto> getWalletShares(@PathVariable Long walletId) {
        Long userId = AuthUtil.getCurrentUserId();
        return walletShareService.getWalletShares(walletId, userId);
    }

    @PostMapping("/wallet/{walletId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public WalletShareDto shareWallet(@PathVariable Long walletId,
                                     @Valid @RequestBody WalletShareDto.CreateWalletShareRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        return walletShareService.shareWallet(walletId, request, userId);
    }

    @PutMapping("/{id}/permission")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public WalletShareDto updatePermission(@PathVariable Long id,
                                           @RequestBody UpdatePermissionRequest request) {
        Long userId = AuthUtil.getCurrentUserId();
        return walletShareService.updatePermission(id, request.getPermission(), userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void unshareWallet(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        walletShareService.unshareWallet(id, userId);
    }

    public static class UpdatePermissionRequest {
        @JsonDeserialize(using = WalletSharePermissionDeserializer.class)
        private WalletShare.Permission permission;

        public WalletShare.Permission getPermission() {
            return permission;
        }

        public void setPermission(WalletShare.Permission permission) {
            this.permission = permission;
        }
    }
}

