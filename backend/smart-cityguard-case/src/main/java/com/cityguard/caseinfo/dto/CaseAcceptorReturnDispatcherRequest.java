package com.cityguard.caseinfo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 受理员认定处置不达标，回退当班派遣员由原部门返工
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CaseAcceptorReturnDispatcherRequest extends CaseReturnRequest {

    /** 当班派遣员用户 ID（必填） */
    private Long dispatcherUserId;
}
