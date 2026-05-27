package com.cityguard.caseinfo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CaseAdjustmentApplyRequest {
    private Long caseId;
    /** extension / suspend */
    private String applyType;
    private String reason;
    /** 挂账截止日期（挂账必填，自今日起不超过 1 年） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate suspendUntil;
    private LocalDateTime clientUpdateTime;
}
