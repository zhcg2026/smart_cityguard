package com.cityguard.appeal.constant;

/**
 * 处置超时申诉常量
 */
public final class TimeoutAppealConstant {

    private TimeoutAppealConstant() {
    }

    public static final String APPLY_TYPE = "timeout_handle";

    public static final String STATUS_PENDING_DISPATCHER = "pending_dispatcher";
    public static final String STATUS_PENDING_ACCEPTOR = "pending_acceptor";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    public static final String RESULT_APPROVED = "approved";
    public static final String RESULT_REJECTED = "rejected";

    public static final String REVIEW_NODE_DISPATCHER = "dispatcher_review";
    public static final String REVIEW_NODE_ACCEPTOR = "acceptor_review";

    public static final String CASE_APPEAL_NONE = "none";
    public static final String CASE_APPEAL_PENDING = "pending";
    public static final String CASE_APPEAL_APPROVED = "approved";
    public static final String CASE_APPEAL_REJECTED = "rejected";
}
