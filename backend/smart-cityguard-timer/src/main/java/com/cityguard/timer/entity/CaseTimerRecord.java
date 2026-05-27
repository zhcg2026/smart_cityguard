package com.cityguard.timer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("case_timer_record")
public class CaseTimerRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long caseId;

    private String caseCode;

    private String timerStage;

    private String stageName;

    private String timeLimitType;

    private Integer timeLimitValue;

    private LocalDateTime startTime;

    private LocalDateTime deadlineTime;

    private LocalDateTime actualFinishTime;

    private Integer totalSeconds;

    private Integer usedSeconds;

    private Integer remainingSeconds;

    private Integer isTimeout;

    private Integer timeoutSeconds;

    private Integer workHoursUsed;

    private Integer workDaysUsed;

    private String timerStatus;

    private LocalDateTime pauseStartTime;

    private Integer totalPausedSeconds;

    private Integer isBundled;

    private String bundledCaseIds;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
