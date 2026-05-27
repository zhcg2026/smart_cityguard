package com.cityguard.common.constant;

/**
 * 案件流转操作类型（写入 case_flow_record.operate_type）
 */
public final class CaseFlowOperateType {

    private CaseFlowOperateType() {
    }

    public static final String FORWARD = "forward";
    public static final String RETURN = "return";
    public static final String CANCEL = "cancel";
    public static final String CLOSE = "close";
    public static final String ASSIGN = "assign";
    public static final String REVOKE_ASSIGN = "revoke_assign";
    public static final String HANDLE = "handle";
}
