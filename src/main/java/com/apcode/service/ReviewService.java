package com.apcode.service;

import com.apcode.dto.CreateReviewRequest;
import com.apcode.dto.ReviewResponse;
import jakarta.validation.Valid;

import java.util.List;

public interface ReviewService {
    List<ReviewResponse> getPublicReviews();

    ReviewResponse createReview(@Valid CreateReviewRequest request, String username);

    ReviewResponse approveReview(Long id);
}
