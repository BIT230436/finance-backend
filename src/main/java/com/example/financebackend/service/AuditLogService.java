package com.example.financebackend.service;

import com.example.financebackend.dto.AuditLogDto;
import com.example.financebackend.entity.AuditLog;
import com.example.financebackend.entity.User;
import com.example.financebackend.repository.AuditLogRepository;
import com.example.financebackend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void log(AuditLog.Action action, Long userId, String entity, Long entityId, String metadata) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setMetadata(metadata);
        if (userId != null) {
            userRepository.findById(userId).ifPresent(log::setUser);
        }
        auditLogRepository.save(log);
    }

    public void log(AuditLog.Action action, Long userId, String entity, Long entityId, String metadata, String ipAddress, String userAgent) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setMetadata(metadata);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        if (userId != null) {
            userRepository.findById(userId).ifPresent(log::setUser);
        }
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getUserLogs(Long userId, Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return new PageImpl<>(
                logs.getContent().stream()
                        .map(this::toDto)
                        .collect(Collectors.toList()),
                pageable,
                logs.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAllLogs(Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return new PageImpl<>(
                logs.getContent().stream()
                        .map(this::toDto)
                        .collect(Collectors.toList()),
                pageable,
                logs.getTotalElements()
        );
    }

    private AuditLogDto toDto(AuditLog log) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(log.getId());
        if (log.getUser() != null) {
            dto.setUserId(log.getUser().getId());
            dto.setUserEmail(log.getUser().getEmail());
            dto.setUserFullName(log.getUser().getFullName());
        }
        dto.setAction(log.getAction());
        dto.setEntity(log.getEntity());
        dto.setEntityId(log.getEntityId());
        dto.setMetadata(log.getMetadata());
        dto.setCreatedAt(log.getCreatedAt());
        dto.setIpAddress(log.getIpAddress());
        dto.setUserAgent(log.getUserAgent());
        return dto;
    }
}
