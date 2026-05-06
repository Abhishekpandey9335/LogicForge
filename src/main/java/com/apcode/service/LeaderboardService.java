package com.apcode.service;

import com.apcode.dto.LeaderboardEntry;

import java.util.List;

public interface LeaderboardService {
    List<LeaderboardEntry> getTopLearners(int min);
}
