package com.example.financebackend.controller;

import com.example.financebackend.dto.NotificationDto;
import com.example.financebackend.service.NotificationService;
import com.example.financebackend.util.AuthUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationDto> list() {
        Long userId = AuthUtil.getCurrentUserId();
        return notificationService.getUserNotifications(userId);
    }

    @GetMapping("/unread")
    public List<NotificationDto> getUnread() {
        Long userId = AuthUtil.getCurrentUserId();
        return notificationService.getUnreadNotifications(userId);
    }

    @GetMapping("/unread/count")
    public UnreadCountResponse getUnreadCount() {
        Long userId = AuthUtil.getCurrentUserId();
        Long count = notificationService.getUnreadCount(userId);
        return new UnreadCountResponse(count);
    }

    @PutMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable Long id) {
        Long userId = AuthUtil.getCurrentUserId();
        notificationService.markAsRead(id, userId);
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead() {
        Long userId = AuthUtil.getCurrentUserId();
        notificationService.markAllAsRead(userId);
    }

    /**
     * TEST ONLY: Create test notification for current user
     */
    @PostMapping("/test")
    @ResponseStatus(HttpStatus.CREATED)
    public void createTestNotification() {
        Long userId = AuthUtil.getCurrentUserId();
        notificationService.createSystemNotification(
            userId, 
            "🔔 Thông báo test", 
            "Đây là thông báo test để kiểm tra tính năng notification. Nếu bạn thấy được thông báo này, tức là tính năng đang hoạt động tốt!"
        );
    }

    public static class UnreadCountResponse {
        private Long count;

        public UnreadCountResponse(Long count) {
            this.count = count;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }
}

