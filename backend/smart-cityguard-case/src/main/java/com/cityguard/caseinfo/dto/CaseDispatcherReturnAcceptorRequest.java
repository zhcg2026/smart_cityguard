package com.cityguard.caseinfo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 派遣员认定非本局职责，回退受理员
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CaseDispatcherReturnAcceptorRequest extends CaseReturnRequest {

    /** 可选；默认优先立案受理员 */
    private Long acceptorUserId;
}
