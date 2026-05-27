package com.cityguard.caseinfo.constant;

public final class CaseAdjustmentConstant {

    public static final String TYPE_EXTENSION = "extension";
    public static final String TYPE_SUSPEND = "suspend";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    public static final int MAX_EXTENSION_APPROVED = 2;
    public static final int MAX_SUSPEND_APPROVED = 1;
    public static final int MAX_SUSPEND_DAYS = 365;

    private CaseAdjustmentConstant() {
    }
}
