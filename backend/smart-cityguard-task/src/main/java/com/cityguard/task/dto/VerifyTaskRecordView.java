package com.cityguard.task.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VerifyTaskRecordView {

    private Long id;

    private String taskCode;

    private String taskStatus;

    private String verifyResult;

    private String verifyResultLabel;

    private String verifyOpinion;

    private String collectorName;

    private String assignRemark;

    private LocalDateTime deadlineTime;

    private LocalDateTime assignTime;

    private LocalDateTime finishTime;

    private List<TaskAttachmentView> attachments;
}
