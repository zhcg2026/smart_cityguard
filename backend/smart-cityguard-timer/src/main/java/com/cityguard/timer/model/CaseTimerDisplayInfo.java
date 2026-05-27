package com.cityguard.timer.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaseTimerDisplayInfo {

    private String timeRemaining;
    private Long handleRemainingSeconds;
    private Boolean handleTimeout;
    private LocalDateTime deadlineTime;
    private String timeLimitType;
    private Integer timeLimitValue;
}
