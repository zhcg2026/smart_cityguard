package com.cityguard.timer.constant;

public final class TimerDefaults {

    /** 受理阶段默认 15 分钟（连续） */
    public static final int ACCEPT_MINUTES = 15;
    /** 派遣阶段默认 15 分钟（连续） */
    public static final int DISPATCH_MINUTES = 15;
    /** 核查/核实任务默认 30 分钟（连续） */
    public static final int TASK_MINUTES = 30;

    public static final String STAGE_ACCEPT = "accept";
    public static final String STAGE_DISPATCH = "dispatch";
    public static final String STAGE_HANDLE = "handle";

    private TimerDefaults() {
    }
}
