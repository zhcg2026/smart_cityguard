package com.cityguard.timer.service;

import com.cityguard.config.entity.CaseStandard;
import com.cityguard.config.mapper.CaseStandardMapper;
import com.cityguard.config.util.CategoryCodeHelper;
import com.cityguard.timer.constant.TimerDefaults;
import com.cityguard.timer.constant.TimerStageConstant;
import com.cityguard.timer.constant.TimerStatusConstant;
import com.cityguard.timer.entity.CaseTimerRecord;
import com.cityguard.timer.entity.HolidayConfig;
import com.cityguard.timer.entity.WorkTimeConfig;
import com.cityguard.timer.mapper.CaseTimerRecordMapper;
import com.cityguard.timer.mapper.HolidayConfigMapper;
import com.cityguard.timer.mapper.WorkTimeConfigMapper;
import com.cityguard.timer.model.CaseTimerDisplayInfo;
import com.cityguard.timer.model.CaseTimerStageDisplay;
import com.cityguard.timer.model.ResolvedTimeLimit;
import com.cityguard.timer.model.TimerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseTimerService {

    private final CaseTimerRecordMapper caseTimerRecordMapper;
    private final WorkTimeConfigMapper workTimeConfigMapper;
    private final HolidayConfigMapper holidayConfigMapper;
    private final CaseStandardMapper caseStandardMapper;
    private final JdbcTemplate jdbcTemplate;

    /** 上报进入待立案：启动受理计时（15分钟连续） */
    @Transactional
    public void onCaseReported(Long caseId, String caseCode, LocalDateTime startTime) {
        startStageTimer(caseId, caseCode, TimerStageConstant.ACCEPT, "受理",
                "urgent_minute", TimerDefaults.ACCEPT_MINUTES, startTime);
    }

    /** 立案批转派遣员：结束受理，启动派遣计时 */
    @Transactional
    public void onCaseRegistered(Long caseId, String caseCode, LocalDateTime finishTime) {
        finishStageTimer(caseId, TimerStageConstant.ACCEPT, finishTime);
        startStageTimer(caseId, caseCode, TimerStageConstant.DISPATCH, "派遣",
                "urgent_minute", TimerDefaults.DISPATCH_MINUTES, finishTime);
    }

    /**
     * 派遣至部门：结束派遣计时，启动处置计时（若已有进行中的处置计时不重置，退回重办继续计时）。
     */
    @Transactional
    public void onCaseDispatched(Long caseId, String caseCode, Long smallId, Long standardId,
                                 LocalDateTime dispatchTime) {
        finishStageTimer(caseId, TimerStageConstant.DISPATCH, dispatchTime);
        if (hasActiveHandleTimer(caseId)) {
            log.debug("Case {} handle timer already running, skip restart", caseId);
            return;
        }
        ResolvedTimeLimit limit = resolveHandleTimeLimit(smallId, standardId);
        startStageTimer(caseId, caseCode, TimerStageConstant.HANDLE, "处置",
                limit.getTimeLimitType(), limit.getTimeLimitValue(), dispatchTime);
        syncCaseInfoDeadline(caseId, limit, dispatchTime);
    }

    /** 部门批转派遣员：结束处置计时 */
    @Transactional
    public void onCaseHandleFinished(Long caseId, LocalDateTime finishTime) {
        finishStageTimer(caseId, TimerStageConstant.HANDLE, finishTime);
    }

    /** 核查/核实任务截止（30分钟连续） */
    public LocalDateTime taskDeadline(LocalDateTime assignTime) {
        return assignTime.plusMinutes(TimerDefaults.TASK_MINUTES);
    }

    /** 下发核查：暂停受理计时，启动核查任务计时（30分钟连续） */
    @Transactional
    public void onCheckTaskAssigned(Long caseId, String caseCode, LocalDateTime assignTime) {
        pauseStageTimer(caseId, TimerStageConstant.ACCEPT);
        startStageTimer(caseId, caseCode, TimerStageConstant.CHECK, "核查",
                "urgent_minute", TimerDefaults.TASK_MINUTES, assignTime);
    }

    /** 核查完成：结束核查计时，恢复受理计时 */
    @Transactional
    public void onCheckTaskCompleted(Long caseId, LocalDateTime finishTime) {
        finishStageTimer(caseId, TimerStageConstant.CHECK, finishTime);
        resumeStageTimer(caseId, TimerStageConstant.ACCEPT);
    }

    /** 下发核实：暂停受理计时，启动核实任务计时（30分钟连续） */
    @Transactional
    public void onVerifyTaskAssigned(Long caseId, String caseCode, LocalDateTime assignTime) {
        pauseStageTimer(caseId, TimerStageConstant.ACCEPT);
        startStageTimer(caseId, caseCode, TimerStageConstant.VERIFY, "核实",
                "urgent_minute", TimerDefaults.TASK_MINUTES, assignTime);
    }

    /** 核实完成：结束核实计时，恢复受理计时 */
    @Transactional
    public void onVerifyTaskCompleted(Long caseId, LocalDateTime finishTime) {
        finishStageTimer(caseId, TimerStageConstant.VERIFY, finishTime);
        resumeStageTimer(caseId, TimerStageConstant.ACCEPT);
    }

    /**
     * 派遣员批转受理员结案：启动结案阶段受理计时（15分钟连续）。
     * 与立案前受理为同一 stage 的不同轮次，以最新一条 active 记录为准。
     */
    @Transactional
    public void onCaseAcceptorPendingClose(Long caseId, String caseCode, LocalDateTime startTime) {
        if (caseTimerRecordMapper.selectActiveByCaseAndStage(caseId, TimerStageConstant.ACCEPT) != null) {
            return;
        }
        startStageTimer(caseId, caseCode, TimerStageConstant.ACCEPT, "受理",
                "urgent_minute", TimerDefaults.ACCEPT_MINUTES, startTime);
    }

    /** 结案：结束结案阶段受理计时 */
    @Transactional
    public void onCaseClosed(Long caseId, LocalDateTime finishTime) {
        finishStageTimer(caseId, TimerStageConstant.ACCEPT, finishTime);
    }

    public boolean hasActiveHandleTimer(Long caseId) {
        return caseTimerRecordMapper.selectActiveByCaseAndStage(caseId, TimerStageConstant.HANDLE) != null;
    }

    /**
     * 处置时限：以立案条件（case_standard）为准；无 standardId 时默认 4 工作时。
     * 小类覆盖表 category_time_limit_override 已停用，请在案件分类-立案条件中维护。
     */
    public ResolvedTimeLimit resolveHandleTimeLimit(Long smallId, Long standardId) {
        if (standardId != null) {
            CaseStandard standard = caseStandardMapper.selectById(standardId);
            ResolvedTimeLimit resolved = HandleTimeLimitNormalizer.fromCaseStandard(standard);
            if (resolved != null) {
                return resolved;
            }
        }
        return new ResolvedTimeLimit("work_hour", 4, false);
    }

    /**
     * 延期：在当前 deadline 基础上再加一个原处置时限，并同步 case_info.deadline_time。
     */
    @Transactional
    public LocalDateTime extendHandleDeadline(Long caseId) {
        CaseTimerRecord record = caseTimerRecordMapper.selectActiveByCaseAndStage(caseId, TimerStageConstant.HANDLE);
        if (record == null) {
            record = caseTimerRecordMapper.selectLatestByCaseAndStage(caseId, TimerStageConstant.HANDLE);
        }
        if (record == null) {
            throw new com.cityguard.common.exception.BusinessException("未找到处置阶段计时记录，无法延期");
        }
        if (!TimerStatusConstant.RUNNING.equals(record.getTimerStatus())
                && !TimerStatusConstant.PAUSED.equals(record.getTimerStatus())) {
            throw new com.cityguard.common.exception.BusinessException("处置计时已结束，无法延期");
        }
        LocalDateTime currentDeadline = record.getDeadlineTime();
        if (currentDeadline == null) {
            throw new com.cityguard.common.exception.BusinessException("案件截止时间为空，无法延期");
        }
        TimerContext ctx = loadContext();
        LocalDateTime newDeadline = DeadlineCalculator.calculateDeadline(
                currentDeadline, record.getTimeLimitType(), record.getTimeLimitValue(), ctx);
        int extraSeconds = DeadlineCalculator.calculateTotalSeconds(
                record.getTimeLimitType(), record.getTimeLimitValue());
        int total = record.getTotalSeconds() != null ? record.getTotalSeconds() : extraSeconds;
        record.setDeadlineTime(newDeadline);
        record.setTotalSeconds(total + extraSeconds);
        caseTimerRecordMapper.updateById(record);
        jdbcTemplate.update("UPDATE case_info SET deadline_time = ? WHERE id = ?", newDeadline, caseId);
        return newDeadline;
    }

    /** 挂账：暂停处置计时 */
    @Transactional
    public void pauseHandleTimer(Long caseId) {
        pauseStageTimer(caseId, TimerStageConstant.HANDLE);
    }

    /** 挂账恢复：继续处置计时，截止顺延 */
    @Transactional
    public void resumeHandleTimer(Long caseId) {
        resumeStageTimer(caseId, TimerStageConstant.HANDLE);
    }

    /** 暂停指定阶段计时（核查/核实等待采集员反馈时暂停受理） */
    @Transactional
    public void pauseStageTimer(Long caseId, String stage) {
        CaseTimerRecord record = caseTimerRecordMapper.selectActiveByCaseAndStage(caseId, stage);
        if (record == null || TimerStatusConstant.PAUSED.equals(record.getTimerStatus())) {
            return;
        }
        record.setTimerStatus(TimerStatusConstant.PAUSED);
        record.setPauseStartTime(LocalDateTime.now());
        caseTimerRecordMapper.updateById(record);
    }

    /** 恢复指定阶段计时，暂停期间不计入已用时长 */
    @Transactional
    public void resumeStageTimer(Long caseId, String stage) {
        CaseTimerRecord record = caseTimerRecordMapper.selectActiveByCaseAndStage(caseId, stage);
        if (record == null || !TimerStatusConstant.PAUSED.equals(record.getTimerStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int paused = record.getTotalPausedSeconds() != null ? record.getTotalPausedSeconds() : 0;
        if (record.getPauseStartTime() != null) {
            paused += (int) Duration.between(record.getPauseStartTime(), now).getSeconds();
        }
        record.setTotalPausedSeconds(paused);
        record.setPauseStartTime(null);
        record.setTimerStatus(TimerStatusConstant.RUNNING);
        TimerContext ctx = loadContext();
        LocalDateTime newDeadline = DeadlineCalculator.calculateDeadline(
                record.getStartTime(), record.getTimeLimitType(), record.getTimeLimitValue(), ctx);
        if (paused > 0 && DeadlineCalculator.isContinuous(record.getTimeLimitType())) {
            newDeadline = newDeadline.plusSeconds(paused);
        } else if (paused > 0) {
            newDeadline = DeadlineCalculator.addWorkingSeconds(newDeadline, paused, ctx);
        }
        record.setDeadlineTime(newDeadline);
        caseTimerRecordMapper.updateById(record);
        if (TimerStageConstant.HANDLE.equals(stage)) {
            jdbcTemplate.update(
                    "UPDATE case_info SET deadline_time = ? WHERE id = ?",
                    newDeadline, caseId);
        }
    }

    public CaseTimerDisplayInfo buildCaseTimerDisplay(Long caseId) {
        if (caseId == null) {
            return null;
        }
        CaseTimerRecord record = resolveDisplayTimerRecord(caseId);
        if (record == null) {
            return null;
        }
        CaseTimerDisplayInfo info = new CaseTimerDisplayInfo();
        info.setTimerStage(record.getTimerStage());
        info.setStageName(record.getStageName());
        info.setTimeLimitType(record.getTimeLimitType());
        info.setTimeLimitValue(record.getTimeLimitValue());
        info.setDeadlineTime(record.getDeadlineTime());
        fillRunningOrFinishedDisplay(caseId, record, info);
        return info;
    }

    /** 详情：受理 / 派遣 / 处置各阶段计时（按阶段顺序，仅有记录的才返回） */
    public List<CaseTimerStageDisplay> buildCaseTimerStages(Long caseId) {
        if (caseId == null) {
            return List.of();
        }
        List<CaseTimerStageDisplay> list = new ArrayList<>(5);
        for (String stage : new String[]{
                TimerStageConstant.ACCEPT,
                TimerStageConstant.DISPATCH,
                TimerStageConstant.HANDLE,
                TimerStageConstant.CHECK,
                TimerStageConstant.VERIFY
        }) {
            CaseTimerRecord record = caseTimerRecordMapper.selectLatestByCaseAndStage(caseId, stage);
            if (record == null) {
                continue;
            }
            list.add(toStageDisplay(caseId, record));
        }
        return list;
    }

    public boolean wasHandleStageTimedOut(Long caseId) {
        CaseTimerRecord timer = caseTimerRecordMapper.selectLatestByCaseAndStage(
                caseId, TimerStageConstant.HANDLE);
        return timer != null && timer.getIsTimeout() != null && timer.getIsTimeout() == 1;
    }

    /**
     * 处置阶段是否已超时（进行中已超过 deadline，或计时记录已标记超时）。
     * 用于延期/挂账申请准入：已超时不可再申请。
     */
    public boolean isHandleStageOverdue(Long caseId) {
        if (caseId == null) {
            return false;
        }
        CaseTimerRecord record = caseTimerRecordMapper.selectActiveByCaseAndStage(
                caseId, TimerStageConstant.HANDLE);
        if (record == null) {
            record = caseTimerRecordMapper.selectLatestByCaseAndStage(caseId, TimerStageConstant.HANDLE);
        }
        if (record != null) {
            if (TimerStatusConstant.RUNNING.equals(record.getTimerStatus())
                    || TimerStatusConstant.PAUSED.equals(record.getTimerStatus())) {
                return record.getDeadlineTime() != null
                        && record.getDeadlineTime().isBefore(LocalDateTime.now());
            }
            return record.getIsTimeout() != null && record.getIsTimeout() == 1;
        }
        List<LocalDateTime> deadlines = jdbcTemplate.query(
                """
                SELECT deadline_time FROM case_info
                WHERE id = ? AND is_deleted = 0
                  AND case_status IN ('pending_handle', 'handling')
                """,
                (rs, rowNum) -> {
                    var ts = rs.getTimestamp("deadline_time");
                    return ts != null ? ts.toLocalDateTime() : null;
                },
                caseId);
        if (deadlines.isEmpty() || deadlines.get(0) == null) {
            return false;
        }
        return deadlines.get(0).isBefore(LocalDateTime.now());
    }

    private boolean isHandleTimeoutExempt(Long caseId) {
        List<Integer> rows = jdbcTemplate.query(
                "SELECT handle_timeout_exempt FROM case_info WHERE id = ? AND is_deleted = 0",
                (rs, rowNum) -> rs.getInt(1), caseId);
        return !rows.isEmpty() && rows.get(0) == 1;
    }

    /** 列表展示：优先进行中的核查/核实；否则处置；再受理/派遣 */
    private CaseTimerRecord resolveDisplayTimerRecord(Long caseId) {
        for (String stage : new String[]{
                TimerStageConstant.CHECK,
                TimerStageConstant.VERIFY
        }) {
            CaseTimerRecord active = caseTimerRecordMapper.selectActiveByCaseAndStage(caseId, stage);
            if (active != null) {
                return active;
            }
        }
        CaseTimerRecord handleActive = caseTimerRecordMapper.selectActiveByCaseAndStage(
                caseId, TimerStageConstant.HANDLE);
        if (handleActive != null) {
            return handleActive;
        }
        for (String stage : new String[]{
                TimerStageConstant.DISPATCH,
                TimerStageConstant.ACCEPT
        }) {
            CaseTimerRecord active = caseTimerRecordMapper.selectActiveByCaseAndStage(caseId, stage);
            if (active != null) {
                return active;
            }
        }
        return null;
    }

    private void fillRunningOrFinishedDisplay(Long caseId, CaseTimerRecord record, CaseTimerDisplayInfo info) {
        LocalDateTime now = LocalDateTime.now();
        if (TimerStatusConstant.RUNNING.equals(record.getTimerStatus())
                || TimerStatusConstant.PAUSED.equals(record.getTimerStatus())) {
            long remainSec = computeActiveRemainingSeconds(record, now);
            info.setHandleRemainingSeconds(remainSec);
            info.setTimeRemaining(formatRemaining(remainSec));
            boolean overdue = remainSec < 0;
            info.setStageTimeout(overdue);
            if (TimerStageConstant.HANDLE.equals(record.getTimerStage())) {
                info.setHandleTimeout(overdue);
            }
        } else if (record.getActualFinishTime() != null) {
            boolean wasTimeout = record.getIsTimeout() != null && record.getIsTimeout() == 1;
            boolean exempt = TimerStageConstant.HANDLE.equals(record.getTimerStage()) && isHandleTimeoutExempt(caseId);
            if (exempt && wasTimeout) {
                info.setStageTimeout(false);
                info.setHandleTimeout(false);
                info.setTimeRemaining("曾超时（申诉通过，不计入考核）");
            } else {
                info.setStageTimeout(wasTimeout);
                if (TimerStageConstant.HANDLE.equals(record.getTimerStage())) {
                    info.setHandleTimeout(wasTimeout);
                }
                info.setTimeRemaining(wasTimeout ? "已超时" : "按时完成");
            }
        }
    }

    private CaseTimerStageDisplay toStageDisplay(Long caseId, CaseTimerRecord record) {
        CaseTimerStageDisplay item = new CaseTimerStageDisplay();
        item.setTimerStage(record.getTimerStage());
        item.setStageName(record.getStageName());
        item.setStartTime(record.getStartTime());
        item.setDeadlineTime(record.getDeadlineTime());
        item.setTimerStatus(record.getTimerStatus());
        item.setActive(TimerStatusConstant.RUNNING.equals(record.getTimerStatus())
                || TimerStatusConstant.PAUSED.equals(record.getTimerStatus()));
        if (record.getTimeLimitType() != null) {
            item.setContinuous(DeadlineCalculator.isContinuous(record.getTimeLimitType()));
            item.setTimeLimitLabel(CategoryCodeHelper.formatHandleTimeLimitLabel(
                    record.getTimeLimitType(), record.getTimeLimitValue()));
        }

        LocalDateTime now = LocalDateTime.now();
        if (Boolean.TRUE.equals(item.getActive())) {
            long remainSec = computeActiveRemainingSeconds(record, now);
            if (TimerStatusConstant.PAUSED.equals(record.getTimerStatus())) {
                item.setTimeRemaining("已暂停 · " + formatRemaining(remainSec));
            } else {
                item.setTimeRemaining(formatRemaining(remainSec));
            }
            item.setTimedOut(remainSec < 0);
        } else if (record.getActualFinishTime() != null) {
            boolean wasTimeout = record.getIsTimeout() != null && record.getIsTimeout() == 1;
            boolean exempt = TimerStageConstant.HANDLE.equals(record.getTimerStage()) && isHandleTimeoutExempt(caseId);
            if (exempt && wasTimeout) {
                item.setTimedOut(false);
                item.setTimeRemaining("曾超时（申诉通过，不计入考核）");
            } else {
                item.setTimedOut(wasTimeout);
                item.setTimeRemaining(wasTimeout ? "已超时" : "按时完成");
            }
        } else {
            item.setTimeRemaining("--");
            item.setTimedOut(false);
        }
        return item;
    }

    private void startStageTimer(Long caseId, String caseCode, String stage, String stageName,
                                   String timeLimitType, int timeLimitValue, LocalDateTime startTime) {
        TimerContext ctx = loadContext();
        LocalDateTime deadline = DeadlineCalculator.calculateDeadline(startTime, timeLimitType, timeLimitValue, ctx);
        int totalSeconds = DeadlineCalculator.calculateTotalSeconds(timeLimitType, timeLimitValue);

        CaseTimerRecord record = new CaseTimerRecord();
        record.setCaseId(caseId);
        record.setCaseCode(caseCode);
        record.setTimerStage(stage);
        record.setStageName(stageName);
        record.setTimeLimitType(timeLimitType);
        record.setTimeLimitValue(timeLimitValue);
        record.setStartTime(startTime);
        record.setDeadlineTime(deadline);
        record.setTotalSeconds(totalSeconds);
        record.setRemainingSeconds(totalSeconds);
        record.setTimerStatus(TimerStatusConstant.RUNNING);
        record.setTotalPausedSeconds(0);
        record.setIsTimeout(0);
        record.setIsBundled(0);
        caseTimerRecordMapper.insert(record);
    }

    private void finishStageTimer(Long caseId, String stage, LocalDateTime finishTime) {
        CaseTimerRecord record = caseTimerRecordMapper.selectActiveByCaseAndStage(caseId, stage);
        if (record == null) {
            return;
        }
        TimerContext ctx = loadContext();
        int paused = record.getTotalPausedSeconds() != null ? record.getTotalPausedSeconds() : 0;
        if (TimerStatusConstant.PAUSED.equals(record.getTimerStatus()) && record.getPauseStartTime() != null) {
            paused += (int) Duration.between(record.getPauseStartTime(), finishTime).getSeconds();
        }
        int used = DeadlineCalculator.calculateUsedSeconds(
                record.getStartTime(), finishTime, record.getTimeLimitType(), ctx, paused);
        int total = record.getTotalSeconds() != null ? record.getTotalSeconds() : 0;
        boolean timeout = finishTime.isAfter(record.getDeadlineTime());
        int timeoutSec = timeout ? (int) Duration.between(record.getDeadlineTime(), finishTime).getSeconds() : 0;

        record.setActualFinishTime(finishTime);
        record.setUsedSeconds(used);
        record.setRemainingSeconds(Math.max(0, total - used));
        record.setIsTimeout(timeout ? 1 : 0);
        record.setTimeoutSeconds(timeoutSec);
        record.setTimerStatus(timeout ? TimerStatusConstant.TIMEOUT : TimerStatusConstant.FINISHED);
        record.setPauseStartTime(null);
        record.setTotalPausedSeconds(paused);
        caseTimerRecordMapper.updateById(record);
    }

    private void syncCaseInfoDeadline(Long caseId, ResolvedTimeLimit limit, LocalDateTime startTime) {
        TimerContext ctx = loadContext();
        LocalDateTime deadline = DeadlineCalculator.calculateDeadline(
                startTime, limit.getTimeLimitType(), limit.getTimeLimitValue(), ctx);
        jdbcTemplate.update("""
                UPDATE case_info SET time_limit_type = ?, time_limit_value = ?, deadline_time = ?, is_urgent = ?
                WHERE id = ?
                """,
                limit.getTimeLimitType(),
                limit.getTimeLimitValue(),
                deadline,
                limit.isUrgent() ? 1 : 0,
                caseId);
    }

    private TimerContext loadContext() {
        WorkTimeConfig work = workTimeConfigMapper.selectDefault();
        Map<String, String> holidays = new HashMap<>();
        int year = LocalDateTime.now().getYear();
        List<HolidayConfig> list = holidayConfigMapper.selectByYear(year);
        for (HolidayConfig h : list) {
            if (h.getHolidayDate() != null && h.getHolidayType() != null) {
                holidays.put(h.getHolidayDate().toString(), h.getHolidayType());
            }
        }
        return TimerContext.from(work, holidays);
    }

    /** 暂停期间冻结剩余展示，恢复后 deadline 已顺延 */
    private static long computeActiveRemainingSeconds(CaseTimerRecord record, LocalDateTime now) {
        long remainSec = Duration.between(now, record.getDeadlineTime()).getSeconds();
        if (TimerStatusConstant.PAUSED.equals(record.getTimerStatus()) && record.getPauseStartTime() != null) {
            remainSec += Duration.between(record.getPauseStartTime(), now).getSeconds();
        }
        return remainSec;
    }

    private static String formatRemaining(long remainSec) {
        if (remainSec < 0) {
            return "超时" + formatDuration(Math.abs(remainSec));
        }
        if (remainSec >= 86400) {
            long days = remainSec / 86400;
            long rest = remainSec % 86400;
            if (rest > 0) {
                return "剩余" + days + "天" + formatDuration(rest);
            }
            return "剩余" + days + "天";
        }
        return "剩余" + formatDuration(remainSec);
    }

    /** 剩余/已用时长：满 1 小时带分钟，避免 1 小时 55 分显示成「1 小时」 */
    private static String formatDuration(long sec) {
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        if (h > 0 && m > 0) {
            return h + "小时" + m + "分";
        }
        if (h > 0) {
            return h + "小时";
        }
        return Math.max(1, m) + "分钟";
    }
}
