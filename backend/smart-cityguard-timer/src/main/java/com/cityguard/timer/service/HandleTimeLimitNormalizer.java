package com.cityguard.timer.service;

import com.cityguard.config.entity.CaseStandard;
import com.cityguard.config.util.CategoryCodeHelper;
import com.cityguard.config.util.CategoryCodeHelper.ParsedHandleTime;
import com.cityguard.timer.model.ResolvedTimeLimit;

/**
 * 从立案条件解析处置时限；当 {@code handle_time_type} 与 {@code handle_time_limit} 文案不一致时，
 * 以文案为准（含「紧急」→ 连续计时），兼容 muban 解析修正前导入的旧数据。
 */
public final class HandleTimeLimitNormalizer {

    private HandleTimeLimitNormalizer() {
    }

    public static ResolvedTimeLimit fromCaseStandard(CaseStandard standard) {
        if (standard == null) {
            return null;
        }
        ParsedHandleTime parsed = CategoryCodeHelper.resolveHandleTime(
                standard.getHandleTimeType(),
                standard.getHandleTimeValue(),
                standard.getHandleTimeLimit());
        if (parsed == null) {
            return null;
        }
        String type = parsed.type();
        return new ResolvedTimeLimit(type, parsed.value(), DeadlineCalculator.isContinuous(type));
    }
}
