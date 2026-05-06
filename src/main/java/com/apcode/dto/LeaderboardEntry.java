package com.apcode.dto;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LeaderboardEntry {
    private Integer rank;
    private Long userId;
    private String name;
    private String city;
    private Integer totalPoints;
    private Integer streakDays;
}
