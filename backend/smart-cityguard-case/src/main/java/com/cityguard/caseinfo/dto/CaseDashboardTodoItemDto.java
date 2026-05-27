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

    private LocalDateTime deadline;

    /** 任务类型：verify | check（type=task 时） */
    private String taskType;

    private Long caseId;
}
