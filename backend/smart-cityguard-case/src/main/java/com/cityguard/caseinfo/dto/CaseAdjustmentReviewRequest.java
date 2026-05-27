package com.cityguard.caseinfo.dto;

import lombok.Data;

@Data
public class CaseAdjustmentReviewRequest {
    private Long applyId;
    /** true=批准 false=驳回 */
    private Boolean approved;
    private String reviewRemark;
}
