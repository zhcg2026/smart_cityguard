package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处置部门确认处置结果并批转派遣员（派遣员把关后再批转受理员结案）
 */
@Data
public class CaseDeptConfirmRequest {

    private Long caseId;

    /** 批转目标派遣员 */
    private Long dispatcherUserId;

    private String remark;

    private LocalDateTime clientUpdateTime;
}
