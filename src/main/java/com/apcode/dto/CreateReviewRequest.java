package com.apcode.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class CreateReviewRequest {
    @NotBlank private String reviewText;
    @NotNull @Min(1) @Max(5) private Integer rating;
    private Long courseId;
}
