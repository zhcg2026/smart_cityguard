package com.cityguard.appeal.dto;

import com.cityguard.appeal.entity.AppealApply;
import com.cityguard.appeal.entity.AppealAttachment;
import com.cityguard.appeal.entity.AppealReview;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TimeoutAppealDetailVo {

    private AppealApply appeal;
    private List<AppealReview> reviews;
    private List<AppealAttachment> attachments;

    private Long caseId;
    private String caseCode;
    private String caseStatus;
    private String handleDeptName;
    private String address;
    private String description;

    private LocalDateTime reportTime;
    private LocalDateTime closeTime;
    private LocalDateTime handleDeadlineTime;
    private LocalDateTime handleFinishTime;

    /** 处置阶段是否曾超时（计时记录） */
    private Boolean handleStageTimedOut;
    private Integer handleTimeoutSeconds;

    /** 是否已申诉豁免 */
    private Boolean handleTimeoutExempt;

    private boolean canDispatcherReview;
    private boolean canAcceptorReview;
}
