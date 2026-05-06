package com.apcode.service;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public interface NewsletterService {
    String subscribe(@NotBlank @Email(message = "Invalid email address") String email);

    void unsubscribe(@NotBlank @Email(message = "Invalid email address") String email);
}
