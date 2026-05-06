package com.apcode.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class NewsletterRequest {
    @NotBlank @Email(message = "Invalid email address")
    private String email;
}
