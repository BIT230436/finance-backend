package com.example.financebackend.service;

import com.example.financebackend.entity.*;
import com.example.financebackend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Expense Splitting Service
 * Chia bill với bạn bè/gia đình
 */
@Service
@Transactional
public class ExpenseSplittingService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseSplittingService.class);

    private final SplitExpenseRepository splitExpenseRepository;
    private final SplitExpenseParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public ExpenseSplittingService(SplitExpenseRepository splitExpenseRepository,
                                  SplitExpenseParticipantRepository participantRepository,
                                  UserRepository userRepository,
                                  NotificationRepository notificationRepository) {
        this.splitExpenseRepository = splitExpenseRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Create split expense
     */
    public Map<String, Object> createSplitExpense(String description, BigDecimal totalAmount,
                                                  List<Long> participantUserIds, 
                                                  Map<Long, BigDecimal> customAmounts,
                                                  Long creatorUserId) {
        User creator = userRepository.findById(creatorUserId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // Validate total amount
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tổng số tiền phải lớn hơn 0");
        }

        // Create split expense
        SplitExpense splitExpense = new SplitExpense();
        splitExpense.setDescription(description);
        splitExpense.setTotalAmount(totalAmount);
        splitExpense.setCreatedBy(creator);
        splitExpense.setStatus(SplitExpense.SplitStatus.PENDING);
        
        SplitExpense saved = splitExpenseRepository.save(splitExpense);

        // Create participants
        List<SplitExpenseParticipant> participants = new ArrayList<>();
        
        if (customAmounts != null && !customAmounts.isEmpty()) {
            // Custom split
            for (Map.Entry<Long, BigDecimal> entry : customAmounts.entrySet()) {
                Long userId = entry.getKey();
                BigDecimal amount = entry.getValue();
                participants.add(createParticipant(saved, userId, amount));
            }
        } else {
            // Equal split
            int participantCount = participantUserIds.size();
            BigDecimal shareAmount = totalAmount.divide(
                new BigDecimal(participantCount), 2, RoundingMode.HALF_UP);
            
            for (Long userId : participantUserIds) {
                participants.add(createParticipant(saved, userId, shareAmount));
            }
        }

        participantRepository.saveAll(participants);

        // Send notifications to participants
        for (SplitExpenseParticipant participant : participants) {
            if (!participant.getUser().getId().equals(creatorUserId)) {
                sendSplitExpenseNotification(participant, creator);
            }
        }

        logger.info("Created split expense {} with {} participants", saved.getId(), participants.size());

        return buildSplitExpenseResponse(saved, participants);
    }

    /**
     * Mark participant as paid
     */
    public Map<String, Object> markAsPaid(Long splitExpenseId, Long userId) {
        SplitExpenseParticipant participant = participantRepository
            .findBySplitExpenseIdAndUserId(splitExpenseId, userId);
        
        if (participant == null) {
            throw new IllegalArgumentException("Bạn không phải người tham gia split này");
        }

        if (participant.getPaymentStatus() == SplitExpenseParticipant.PaymentStatus.PAID) {
            throw new IllegalArgumentException("Bạn đã thanh toán rồi");
        }

        participant.setPaymentStatus(SplitExpenseParticipant.PaymentStatus.PAID);
        participant.setPaidAt(LocalDateTime.now());
        participantRepository.save(participant);

        // Update split expense status
        updateSplitExpenseStatus(participant.getSplitExpense());

        logger.info("User {} marked split expense {} as paid", userId, splitExpenseId);

        List<SplitExpenseParticipant> allParticipants = 
            participantRepository.findBySplitExpenseId(splitExpenseId);
        return buildSplitExpenseResponse(participant.getSplitExpense(), allParticipants);
    }

    /**
     * Get user's split expenses (as creator or participant)
     */
    public List<Map<String, Object>> getUserSplitExpenses(Long userId) {
        List<SplitExpense> splitExpenses = splitExpenseRepository
            .findByUserIdAsParticipantOrCreator(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (SplitExpense se : splitExpenses) {
            List<SplitExpenseParticipant> participants = 
                participantRepository.findBySplitExpenseId(se.getId());
            result.add(buildSplitExpenseResponse(se, participants));
        }

        return result;
    }

    /**
     * Get pending payments for user
     */
    public List<Map<String, Object>> getPendingPayments(Long userId) {
        List<SplitExpenseParticipant> pending = participantRepository.findPendingByUserId(userId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (SplitExpenseParticipant participant : pending) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", participant.getId());
            item.put("splitExpenseId", participant.getSplitExpense().getId());
            item.put("description", participant.getSplitExpense().getDescription());
            item.put("totalAmount", participant.getSplitExpense().getTotalAmount());
            item.put("yourShare", participant.getShareAmount());
            item.put("createdBy", participant.getSplitExpense().getCreatedBy().getFullName());
            item.put("createdAt", participant.getSplitExpense().getCreatedAt());
            result.add(item);
        }

        return result;
    }

    private SplitExpenseParticipant createParticipant(SplitExpense splitExpense, Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + userId));

        SplitExpenseParticipant participant = new SplitExpenseParticipant();
        participant.setSplitExpense(splitExpense);
        participant.setUser(user);
        participant.setShareAmount(amount);
        participant.setPaymentStatus(SplitExpenseParticipant.PaymentStatus.PENDING);
        
        return participant;
    }

    private void updateSplitExpenseStatus(SplitExpense splitExpense) {
        List<SplitExpenseParticipant> participants = 
            participantRepository.findBySplitExpenseId(splitExpense.getId());

        long paidCount = participants.stream()
            .filter(p -> p.getPaymentStatus() == SplitExpenseParticipant.PaymentStatus.PAID)
            .count();

        if (paidCount == 0) {
            splitExpense.setStatus(SplitExpense.SplitStatus.PENDING);
        } else if (paidCount == participants.size()) {
            splitExpense.setStatus(SplitExpense.SplitStatus.SETTLED);
        } else {
            splitExpense.setStatus(SplitExpense.SplitStatus.PARTIALLY_PAID);
        }

        splitExpenseRepository.save(splitExpense);
    }

    private void sendSplitExpenseNotification(SplitExpenseParticipant participant, User creator) {
        Notification notification = new Notification();
        notification.setUser(participant.getUser());
        notification.setType(Notification.NotificationType.SYSTEM);
        notification.setTitle("💰 Bạn có khoản chi tiêu chung mới");
        notification.setMessage(String.format(
            "%s đã chia sẻ '%s' với bạn. Số tiền của bạn: %s",
            creator.getFullName(),
            participant.getSplitExpense().getDescription(),
            formatMoney(participant.getShareAmount())
        ));
        notification.setRelatedEntityId(participant.getSplitExpense().getId());
        notification.setRelatedEntityType("split_expense");
        
        notificationRepository.save(notification);
    }

    private Map<String, Object> buildSplitExpenseResponse(SplitExpense splitExpense, 
                                                          List<SplitExpenseParticipant> participants) {
        BigDecimal totalPaid = participants.stream()
            .filter(p -> p.getPaymentStatus() == SplitExpenseParticipant.PaymentStatus.PAID)
            .map(SplitExpenseParticipant::getShareAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Safely get creator info with null checks
        User creator = splitExpense.getCreatedBy();
        Map<String, Object> creatorMap = new HashMap<>();
        creatorMap.put("id", creator != null ? creator.getId() : null);
        creatorMap.put("name", creator != null ? creator.getFullName() : "Unknown");
        creatorMap.put("email", creator != null ? creator.getEmail() : "unknown@example.com");

        Map<String, Object> result = new HashMap<>();
        result.put("id", splitExpense.getId());
        result.put("description", splitExpense.getDescription());
        result.put("totalAmount", splitExpense.getTotalAmount());
        result.put("status", splitExpense.getStatus());
        result.put("createdBy", creatorMap);
        result.put("createdAt", splitExpense.getCreatedAt());
        result.put("participants", participants.stream()
            .map(p -> {
                User user = p.getUser();
                Map<String, Object> participantMap = new HashMap<>();
                participantMap.put("id", p.getId());
                participantMap.put("userId", user != null ? user.getId() : null);
                participantMap.put("name", user != null ? user.getFullName() : "Unknown");
                participantMap.put("shareAmount", p.getShareAmount());
                participantMap.put("paymentStatus", p.getPaymentStatus());
                participantMap.put("paidAt", p.getPaidAt() != null ? p.getPaidAt() : "");
                return participantMap;
            })
            .collect(java.util.stream.Collectors.toList())
        );
        result.put("totalPaid", totalPaid);
        result.put("remaining", splitExpense.getTotalAmount().subtract(totalPaid));
        
        return result;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        long amountLong = amount.longValue();
        return String.format("%,d", amountLong).replace(",", ".") + " VND";
    }
}

