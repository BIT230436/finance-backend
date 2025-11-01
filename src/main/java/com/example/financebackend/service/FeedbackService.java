package com.example.financebackend.service;

import com.example.financebackend.dto.FeedbackRequest;
import com.example.financebackend.entity.Feedback;
import com.example.financebackend.entity.User;
import com.example.financebackend.repository.FeedbackRepository;
import com.example.financebackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public FeedbackService(FeedbackRepository feedbackRepository, UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
    }

    public void submitFeedback(FeedbackRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setSubject(request.getSubject());
        // Use message or content (prioritize message)
        String messageContent = request.getMessage() != null ? request.getMessage() : request.getContent();
        if (messageContent == null || messageContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung là bắt buộc");
        }
        feedback.setMessage(messageContent);
        feedback.setType(request.getType() != null ? request.getType() : "FEEDBACK");

        feedbackRepository.save(feedback);
        // TODO: In production, send email to development team
    }
}

