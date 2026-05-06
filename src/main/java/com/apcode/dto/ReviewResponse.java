package com.apcode.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private String reviewerName;
    private String city;
    private Integer rating;
    private String reviewText;
    private String courseTitle;
    private LocalDateTime createdAt;
}
