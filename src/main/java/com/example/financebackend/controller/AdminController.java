package com.example.financebackend.controller;

import com.example.financebackend.dto.UserDto;
import com.example.financebackend.entity.User;
import com.example.financebackend.service.AdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<UserDto> getAllUsers() {
        return adminService.findAllUsers();
    }

    @GetMapping("/users/{id}")
    public UserDto getUser(@PathVariable Long id) {
        return adminService.findUserById(id);
    }

    @PutMapping("/users/{id}/role")
    public UserDto updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return adminService.updateUserRole(id, request.getRole());
    }

    @PutMapping("/users/{id}/enabled")
    public UserDto updateEnabled(@PathVariable Long id, @Valid @RequestBody EnabledUpdateRequest request) {
        return adminService.updateUserEnabled(id, request.getEnabled());
    }

    public static class RoleUpdateRequest {
        @NotNull(message = "Vai trò là bắt buộc")
        private User.Role role;

        public User.Role getRole() {
            return role;
        }

        public void setRole(User.Role role) {
            this.role = role;
        }
    }

    public static class EnabledUpdateRequest {
        @NotNull(message = "Trạng thái kích hoạt là bắt buộc")
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}