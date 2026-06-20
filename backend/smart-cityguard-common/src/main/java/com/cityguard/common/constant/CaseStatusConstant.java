package com.cityguard.common.constant;

/**
 * 案件状态常量
 */
public class CaseStatusConstant {
    // 案件来源
    public static final Integer SOURCE_COLLECTOR_APP = 1;    // 采集员APP
    public static final Integer SOURCE_PHONE = 2;            // 电话
    public static final Integer SOURCE_WEBSITE = 3;          // 网站
    public static final Integer SOURCE_OTHER = 4;            // 其他

    // 上报阶段
    public static final String REPORTED = "reported";           // 上报
    public static final String PENDING_VERIFY = "pending_verify"; // 待核实
    public static final String PENDING_REGISTER = "pending_register"; // 待立案

    // 立案阶段
    public static final String ACCEPTED = "accepted";           // 立案
    public static final String PENDING_DISPATCH = "pending_dispatch"; // 待派遣
    public static final String NOT_ACCEPTED = "not_accepted";   // 不立案

    // 派遣阶段
    public static final String DISPATCHED = "dispatched";       // 已派遣
    public static final String PENDING_HANDLE = "pending_handle"; // 待处置
    public static final String RETURNED = "returned";           // 回退

    // 处置阶段
    public static final String HANDLING = "handling";           // 处置中
    public static final String SUSPENDED = "suspended";         // 挂账中
    public static final String HANDLE_FINISH = "handle_finish"; // 处置完成
    public static final String PENDING_CHECK = "pending_check"; // 待核查

    // 核查阶段
    public static final String CHECKING = "checking";           // 核查中
    public static final String CHECK_PASS = "check_pass";       // 核查通过
    public static final String CHECK_NOT_PASS = "check_not_pass"; // 核查不通过（返工）

    // 结案阶段
    public static final String PENDING_CLOSE = "pending_close"; // 待结案
    public static final String CLOSED = "closed";               // 结案
    public static final String FORCED_CLOSE = "forced_close";   // 强制结案

    // 作废
    public static final String CANCELLED = "cancelled";         // 作废
}