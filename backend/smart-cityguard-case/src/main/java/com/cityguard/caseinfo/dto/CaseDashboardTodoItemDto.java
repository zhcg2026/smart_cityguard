package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaseDashboardTodoItemDto {

    /** case | task */
    private String type;

    private Long id;

    private String title;

    /** 展示用状态文案 */
    private String status;

    /** 当前计时阶段中文名（受理/派遣/处置） */
    private String timerStageName;

    private LocalDateTime deadline;

    /** 任务类型：verify | check（type=task 时） */
    private String taskType;

    private Long caseId;
}
