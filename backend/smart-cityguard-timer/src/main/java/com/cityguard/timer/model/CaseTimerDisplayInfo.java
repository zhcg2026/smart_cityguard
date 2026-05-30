package com.cityguard.timer.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaseTimerDisplayInfo {

    /** 当前列表展示阶段：accept / dispatch / handle */
    private String timerStage;
    private String stageName;
    private String timeRemaining;
    private Long handleRemainingSeconds;
    /** 当前展示阶段是否超时（列表着色用） */
    private Boolean stageTimeout;
    /** 处置阶段是否超时（兼容旧字段，仅 handle 阶段有值） */
    private Boolean handleTimeout;
    private LocalDateTime deadlineTime;
    private String timeLimitType;
    private Integer timeLimitValue;
}
