package com.example.financebackend.controller;

import com.example.financebackend.dto.FeedbackRequest;
import com.example.financebackend.service.FeedbackService;
import com.example.financebackend.util.AuthUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);
    
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping(consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public void submitFeedbackJson(@RequestBody FeedbackRequest request) {
        try {
            // Map content to message if message is null
            if (request.getMessage() == null && request.getContent() != null) {
                request.setMessage(request.getContent());
            }
            
            // Validate manually after mapping
            if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                throw new IllegalArgumentException("Tiêu đề là bắt buộc");
            }
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                throw new IllegalArgumentException("Nội dung là bắt buộc");
            }
            
            logger.info("Received feedback submission (JSON): type={}, subject={}", request.getType(), request.getSubject());
            Long userId = AuthUtil.getCurrentUserId();
            feedbackService.submitFeedback(request, userId);
            logger.info("Feedback submitted successfully by user {}", userId);
        } catch (Exception e) {
            logger.error("Error submitting feedback: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public void submitFeedbackForm(@ModelAttribute FeedbackRequest request) {
        try {
            // Map content to message if message is null (for form data)
            if (request.getMessage() == null && request.getContent() != null) {
                request.setMessage(request.getContent());
            }
            
            // Validate manually after mapping
            if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                throw new IllegalArgumentException("Tiêu đề là bắt buộc");
            }
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                throw new IllegalArgumentException("Nội dung là bắt buộc");
            }
            
            logger.info("Received feedback submission (Form): type={}, subject={}", request.getType(), request.getSubject());
            Long userId = AuthUtil.getCurrentUserId();
            feedbackService.submitFeedback(request, userId);
            logger.info("Feedback submitted successfully by user {}", userId);
        } catch (Exception e) {
            logger.error("Error submitting feedback: {}", e.getMessage(), e);
            throw e;
        }
    }
}

