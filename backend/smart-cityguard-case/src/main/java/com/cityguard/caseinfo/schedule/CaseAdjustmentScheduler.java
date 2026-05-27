package com.cityguard.caseinfo.schedule;

import com.cityguard.caseinfo.service.CaseAdjustmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaseAdjustmentScheduler {

    private final CaseAdjustmentService caseAdjustmentService;

    /** 每分钟检查挂账到期案件 */
    @Scheduled(cron = "0 * * * * ?")
    public void resumeExpiredSuspensions() {
        try {
            caseAdjustmentService.resumeExpiredSuspensions();
        } catch (Exception e) {
            log.warn("挂账到期恢复任务异常: {}", e.getMessage());
        }
    }
}
