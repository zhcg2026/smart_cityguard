package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 派遣员把关通过，批转指定受理员（受理员岗位均可核实/结案）
 */
@Data
public class CaseDispatcherForwardRequest {

    private Long caseId;

    /** 批转目标受理员用户 ID（必填） */
    private Long acceptorUserId;

    private String remark;

    private LocalDateTime clientUpdateTime;
}
