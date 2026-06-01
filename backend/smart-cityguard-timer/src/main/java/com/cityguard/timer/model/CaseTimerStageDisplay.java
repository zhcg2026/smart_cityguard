package com.cityguard.timer.model;

import lombok.Data;

import java.time.LocalDateTime;

/** 案件某一阶段计时（详情展示用） */
@Data
public class CaseTimerStageDisplay {

    private String timerStage;
    private String stageName;
    private LocalDateTime startTime;
    private LocalDateTime deadlineTime;
    /** 剩余/完成/超时文案 */
    private String timeRemaining;
    /** running / paused / finished / timeout */
    private String timerStatus;
    /** 是否当前进行中阶段 */
    private Boolean active;
    /** 本阶段是否已超时（进行中为实时，已结束为历史结果） */
    private Boolean timedOut;
    /** 是否连续计时（自然时间） */
    private Boolean continuous;
    /** 时限规则文案，如 2紧急工作时 */
    private String timeLimitLabel;
}
