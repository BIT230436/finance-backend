package com.example.financebackend.service;

import com.example.financebackend.entity.User;
import com.example.financebackend.repository.UserRepository;
import com.example.financebackend.util.AuthUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class PermissionService {

    private final UserRepository userRepository;

    public PermissionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get permissions for current user based on their role
     */
    public Map<String, Object> getPermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        Map<String, Object> permissions = new HashMap<>();
        permissions.put("role", user.getRole().name());
        permissions.put("userId", userId);
        permissions.put("email", user.getEmail());
        permissions.put("fullName", user.getFullName());

        // Feature permissions based on role
        Map<String, Boolean> features = new HashMap<>();

        if (user.getRole() == User.Role.ADMIN) {
            // ADMIN có tất cả quyền
            features.put("canManageUsers", true);
            features.put("canViewUsers", true);
            features.put("canCreateWallets", true);
            features.put("canUpdateWallets", true);
            features.put("canDeleteWallets", true);
            features.put("canViewWallets", true);
            features.put("canCreateTransactions", true);
            features.put("canUpdateTransactions", true);
            features.put("canDeleteTransactions", true);
            features.put("canViewTransactions", true);
            features.put("canCreateCategories", true);
            features.put("canUpdateCategories", true);
            features.put("canDeleteCategories", true);
            features.put("canViewCategories", true);
            features.put("canCreateBudgets", true);
            features.put("canUpdateBudgets", true);
            features.put("canDeleteBudgets", true);
            features.put("canViewBudgets", true);
            features.put("canCreateGoals", true);
            features.put("canUpdateGoals", true);
            features.put("canDeleteGoals", true);
            features.put("canViewGoals", true);
            features.put("canViewReports", true);
            features.put("canExportData", true);
            features.put("canImportData", true);
            features.put("canManageRecurringTransactions", true);
            features.put("canShareWallets", true);
            features.put("canSendFeedback", true);
        } else if (user.getRole() == User.Role.USER) {
            // USER có quyền quản lý tài chính đầy đủ (không có quyền admin)
            features.put("canManageUsers", false);
            features.put("canViewUsers", false);
            features.put("canCreateWallets", true);
            features.put("canUpdateWallets", true);
            features.put("canDeleteWallets", true);
            features.put("canViewWallets", true);
            features.put("canCreateTransactions", true);
            features.put("canUpdateTransactions", true);
            features.put("canDeleteTransactions", true);
            features.put("canViewTransactions", true);
            features.put("canCreateCategories", true);
            features.put("canUpdateCategories", true);
            features.put("canDeleteCategories", true);
            features.put("canViewCategories", true);
            features.put("canCreateBudgets", true);
            features.put("canUpdateBudgets", true);
            features.put("canDeleteBudgets", true);
            features.put("canViewBudgets", true);
            features.put("canCreateGoals", true);
            features.put("canUpdateGoals", true);
            features.put("canDeleteGoals", true);
            features.put("canViewGoals", true);
            features.put("canViewReports", true);
            features.put("canExportData", true);
            features.put("canImportData", true);
            features.put("canManageRecurringTransactions", true);
            features.put("canShareWallets", true);
            features.put("canSendFeedback", true);
        } else if (user.getRole() == User.Role.VIEWER) {
            // VIEWER chỉ có quyền xem (read-only)
            features.put("canManageUsers", false);
            features.put("canViewUsers", false);
            features.put("canCreateWallets", false);
            features.put("canUpdateWallets", false);
            features.put("canDeleteWallets", false);
            features.put("canViewWallets", true);
            features.put("canCreateTransactions", false);
            features.put("canUpdateTransactions", false);
            features.put("canDeleteTransactions", false);
            features.put("canViewTransactions", true);
            features.put("canCreateCategories", false);
            features.put("canUpdateCategories", false);
            features.put("canDeleteCategories", false);
            features.put("canViewCategories", true);
            features.put("canCreateBudgets", false);
            features.put("canUpdateBudgets", false);
            features.put("canDeleteBudgets", false);
            features.put("canViewBudgets", true);
            features.put("canCreateGoals", false);
            features.put("canUpdateGoals", false);
            features.put("canDeleteGoals", false);
            features.put("canViewGoals", true);
            features.put("canViewReports", true);
            features.put("canExportData", false);
            features.put("canImportData", false);
            features.put("canManageRecurringTransactions", false);
            features.put("canShareWallets", false);
            features.put("canSendFeedback", true); // VIEWER vẫn có thể gửi feedback
        }

        permissions.put("features", features);
        return permissions;
    }
}

