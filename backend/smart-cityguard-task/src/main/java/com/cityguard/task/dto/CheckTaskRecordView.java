package com.cityguard.task.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CheckTaskRecordView {

    private Long id;

    private String taskCode;

    private String taskStatus;

    private String checkResult;

    private String checkResultLabel;

    private String checkOpinion;

    private String collectorName;

    private LocalDateTime assignTime;

    private LocalDateTime finishTime;

    private List<TaskAttachmentView> attachments;
}
