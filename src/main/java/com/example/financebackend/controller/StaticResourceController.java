package com.example.financebackend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handle static resource requests
 * Return 204 No Content instead of 404 for missing static resources
 */
@RestController
public class StaticResourceController {

    @GetMapping({"/favicon.ico", "/*.ico", "/*.png", "/*.jpg", "/manifest.json"})
    public ResponseEntity<Void> handleStaticResource() {
        // Return 204 No Content instead of 404
        // This prevents error logs for missing favicon, etc.
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

