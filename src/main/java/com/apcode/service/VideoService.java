package com.apcode.service;

import com.apcode.dto.CreateVideoRequest;
import com.apcode.dto.VideoResponse;
import jakarta.validation.Valid;

import java.util.List;

public interface VideoService {
    List<VideoResponse> getVideosByCourse(String slug);

    VideoResponse createVideo(@Valid CreateVideoRequest request);

    List<VideoResponse> getFreeVideos();
}
