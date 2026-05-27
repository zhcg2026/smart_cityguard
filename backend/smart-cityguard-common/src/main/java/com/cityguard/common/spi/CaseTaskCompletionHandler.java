package com.cityguard.common.spi;

/**
 * 采集员完成核查/核实任务后回写案件（由 case 模块实现）
 */
public interface CaseTaskCompletionHandler {

    void afterCheckTaskCompleted(Long taskId, String checkResult, String checkOpinion, Long collectorId);

    void afterVerifyTaskCompleted(Long taskId, String verifyResult, String verifyOpinion, Long collectorId);
}
