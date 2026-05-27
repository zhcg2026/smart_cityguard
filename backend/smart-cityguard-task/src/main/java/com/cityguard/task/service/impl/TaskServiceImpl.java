package com.cityguard.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.common.constant.TaskStatusConstant;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.common.spi.CaseTaskCompletionHandler;
import com.cityguard.task.dto.CheckTaskRecordView;
import com.cityguard.task.dto.TaskAttachmentView;
import com.cityguard.task.dto.VerifyTaskRecordView;
import com.cityguard.task.entity.CheckAttachment;
import com.cityguard.task.entity.CheckTask;
import com.cityguard.task.entity.VerifyAttachment;
import com.cityguard.task.entity.VerifyTask;
import com.cityguard.task.mapper.CheckAttachmentMapper;
import com.cityguard.task.mapper.CheckTaskMapper;
import com.cityguard.task.mapper.VerifyAttachmentMapper;
import com.cityguard.task.mapper.VerifyTaskMapper;
import com.cityguard.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final VerifyTaskMapper verifyTaskMapper;
    private final CheckTaskMapper checkTaskMapper;
    private final CheckAttachmentMapper checkAttachmentMapper;
    private final VerifyAttachmentMapper verifyAttachmentMapper;
    private final JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private CaseTaskCompletionHandler caseTaskCompletionHandler;

    @Override
    public VerifyTask getVerifyTaskDetail(Long id) {
        VerifyTask task = verifyTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("核实任务不存在");
        }
        enrichVerifyFromCase(task);
        return task;
    }

    @Override
    public CheckTask getCheckTaskDetail(Long id) {
        CheckTask task = checkTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("核查任务不存在");
        }
        enrichCheckFromCase(task);
        return task;
    }

    private void enrichCheckFromCase(CheckTask task) {
        if (task.getCaseId() == null) {
            return;
        }
        try {
            jdbcTemplate.query(
                    "SELECT description, big_name, small_name FROM case_info WHERE id = ? AND is_deleted = 0",
                    rs -> {
                        if (rs.next()) {
                            task.setDescription(rs.getString("description"));
                            task.setBigName(rs.getString("big_name"));
                            if (task.getSmallName() == null) {
                                task.setSmallName(rs.getString("small_name"));
                            }
                        }
                        return null;
                    },
                    task.getCaseId());
        } catch (Exception ignored) {
            // 仅展示增强，失败不影响主流程
        }
    }

    private void enrichVerifyFromCase(VerifyTask task) {
        if (task.getCaseId() == null) {
            return;
        }
        try {
            jdbcTemplate.query(
                    "SELECT handle_dept_name FROM case_info WHERE id = ? AND is_deleted = 0",
                    rs -> {
                        if (rs.next()) {
                            task.setHandleDeptName(rs.getString("handle_dept_name"));
                        }
                        return null;
                    },
                    task.getCaseId());
        } catch (Exception ignored) {
        }
    }

    @Override
    public Page<VerifyTask> getVerifyTaskList(Integer pageNum, Integer pageSize, Map<String, Object> params) {
        Page<VerifyTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<VerifyTask> wrapper = buildVerifyWrapper(params);
        wrapper.orderByDesc(VerifyTask::getCreateTime);
        return verifyTaskMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<CheckTask> getCheckTaskList(Integer pageNum, Integer pageSize, Map<String, Object> params) {
        Page<CheckTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CheckTask> wrapper = buildCheckWrapper(params);
        wrapper.orderByDesc(CheckTask::getCreateTime);
        return checkTaskMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckTask executeCheckTask(Long taskId, String result, String remark, List<String> attachments, Long collectorId) {
        CheckTask task = getCheckTaskDetail(taskId);
        assertCollector(task.getCollectorId(), collectorId);
        if (!TaskStatusConstant.PENDING.equals(task.getTaskStatus())) {
            throw new BusinessException("核查任务已完成或已取消");
        }
        String dbResult = mapCheckResult(result);
        if ("pass".equals(dbResult) && (attachments == null || attachments.isEmpty())) {
            throw new BusinessException("问题确认存在时请上传现场照片");
        }
        task.setTaskStatus(TaskStatusConstant.DONE);
        task.setCheckResult(dbResult);
        task.setCheckOpinion(remark);
        task.setFinishTime(LocalDateTime.now());
        checkTaskMapper.updateById(task);
        persistCheckAttachments(task, attachments);

        if (caseTaskCompletionHandler != null) {
            caseTaskCompletionHandler.afterCheckTaskCompleted(taskId, dbResult, remark, collectorId);
        }
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VerifyTask executeVerifyTask(Long taskId, String result, String remark, List<String> attachments, Long collectorId) {
        VerifyTask task = getVerifyTaskDetail(taskId);
        assertCollector(task.getCollectorId(), collectorId);
        if (!TaskStatusConstant.PENDING.equals(task.getTaskStatus())) {
            throw new BusinessException("核实任务已完成或已取消");
        }
        String dbResult = mapVerifyResult(result);
        if ("exist".equals(dbResult) && (attachments == null || attachments.isEmpty())) {
            throw new BusinessException("处置到位时请上传核实现场照片");
        }
        task.setTaskStatus(TaskStatusConstant.DONE);
        task.setVerifyResult(dbResult);
        task.setVerifyOpinion(remark);
        task.setFinishTime(LocalDateTime.now());
        verifyTaskMapper.updateById(task);
        persistVerifyAttachments(task, attachments);

        if (caseTaskCompletionHandler != null) {
            caseTaskCompletionHandler.afterVerifyTaskCompleted(taskId, dbResult, remark, collectorId);
        }
        return task;
    }

    @Override
    public boolean hasPendingCheckTask(Long caseId) {
        if (caseId == null) {
            return false;
        }
        Long cnt = checkTaskMapper.selectCount(new LambdaQueryWrapper<CheckTask>()
                .eq(CheckTask::getCaseId, caseId)
                .eq(CheckTask::getTaskStatus, TaskStatusConstant.PENDING));
        return cnt != null && cnt > 0;
    }

    @Override
    public List<CheckTaskRecordView> listCheckTaskRecordsByCaseId(Long caseId) {
        if (caseId == null) {
            return List.of();
        }
        List<CheckTask> tasks = checkTaskMapper.selectList(new LambdaQueryWrapper<CheckTask>()
                .eq(CheckTask::getCaseId, caseId)
                .orderByDesc(CheckTask::getAssignTime));
        List<CheckTaskRecordView> views = new ArrayList<>();
        for (CheckTask task : tasks) {
            CheckTaskRecordView view = new CheckTaskRecordView();
            view.setId(task.getId());
            view.setTaskCode(task.getTaskCode());
            view.setTaskStatus(task.getTaskStatus());
            view.setCheckResult(task.getCheckResult());
            view.setCheckResultLabel(labelCheckResult(task.getCheckResult()));
            view.setCheckOpinion(task.getCheckOpinion());
            view.setCollectorName(task.getCollectorName());
            view.setAssignTime(task.getAssignTime());
            view.setFinishTime(task.getFinishTime());
            view.setAttachments(toAttachmentViews(checkAttachmentMapper.selectByCheckTaskId(task.getId())));
            views.add(view);
        }
        return views;
    }

    @Override
    public List<VerifyTaskRecordView> listVerifyTaskRecordsByCaseId(Long caseId) {
        if (caseId == null) {
            return List.of();
        }
        List<VerifyTask> tasks = verifyTaskMapper.selectList(new LambdaQueryWrapper<VerifyTask>()
                .eq(VerifyTask::getCaseId, caseId)
                .orderByDesc(VerifyTask::getAssignTime));
        List<VerifyTaskRecordView> views = new ArrayList<>();
        for (VerifyTask task : tasks) {
            VerifyTaskRecordView view = new VerifyTaskRecordView();
            view.setId(task.getId());
            view.setTaskCode(task.getTaskCode());
            view.setTaskStatus(task.getTaskStatus());
            view.setVerifyResult(task.getVerifyResult());
            view.setVerifyResultLabel(labelVerifyResult(task.getVerifyResult()));
            view.setVerifyOpinion(task.getVerifyOpinion());
            view.setCollectorName(task.getCollectorName());
            view.setAssignTime(task.getAssignTime());
            view.setFinishTime(task.getFinishTime());
            view.setAttachments(toAttachmentViews(verifyAttachmentMapper.selectByVerifyTaskId(task.getId())));
            views.add(view);
        }
        return views;
    }

    @Override
    public boolean hasPendingVerifyTask(Long caseId) {
        if (caseId == null) {
            return false;
        }
        Long cnt = verifyTaskMapper.selectCount(new LambdaQueryWrapper<VerifyTask>()
                .eq(VerifyTask::getCaseId, caseId)
                .eq(VerifyTask::getTaskStatus, TaskStatusConstant.PENDING));
        return cnt != null && cnt > 0;
    }

    private LambdaQueryWrapper<VerifyTask> buildVerifyWrapper(Map<String, Object> params) {
        LambdaQueryWrapper<VerifyTask> wrapper = new LambdaQueryWrapper<>();
        if (params == null) {
            return wrapper;
        }
        if (params.get("status") != null) {
            String st = toTaskStatus(params.get("status"));
            if (st != null) {
                wrapper.eq(VerifyTask::getTaskStatus, st);
            }
        }
        if (params.get("collectorId") != null) {
            wrapper.eq(VerifyTask::getCollectorId, params.get("collectorId"));
        }
        if (params.get("caseId") != null) {
            wrapper.eq(VerifyTask::getCaseId, params.get("caseId"));
        }
        if (params.get("caseCode") != null) {
            String code = params.get("caseCode").toString().trim();
            if (!code.isEmpty()) {
                wrapper.like(VerifyTask::getCaseCode, code);
            }
        }
        return wrapper;
    }

    private LambdaQueryWrapper<CheckTask> buildCheckWrapper(Map<String, Object> params) {
        LambdaQueryWrapper<CheckTask> wrapper = new LambdaQueryWrapper<>();
        if (params == null) {
            return wrapper;
        }
        if (params.get("status") != null) {
            String st = toTaskStatus(params.get("status"));
            if (st != null) {
                wrapper.eq(CheckTask::getTaskStatus, st);
            }
        }
        if (params.get("collectorId") != null) {
            wrapper.eq(CheckTask::getCollectorId, params.get("collectorId"));
        }
        if (params.get("caseId") != null) {
            wrapper.eq(CheckTask::getCaseId, params.get("caseId"));
        }
        if (params.get("caseCode") != null) {
            String code = params.get("caseCode").toString().trim();
            if (!code.isEmpty()) {
                wrapper.like(CheckTask::getCaseCode, code);
            }
        }
        return wrapper;
    }

    private static String toTaskStatus(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.intValue() == 0 ? TaskStatusConstant.PENDING : TaskStatusConstant.DONE;
        }
        String s = raw.toString().trim();
        if ("0".equals(s)) {
            return TaskStatusConstant.PENDING;
        }
        if ("1".equals(s)) {
            return TaskStatusConstant.DONE;
        }
        return s;
    }

    private static void assertCollector(Long assignedId, Long operatorId) {
        if (assignedId == null) {
            throw new BusinessException("任务未指派采集员");
        }
        if (operatorId != null && !assignedId.equals(operatorId)) {
            throw new BusinessException("仅被指派的采集员可提交该任务");
        }
    }

    /** 移动端：confirmed / not_found / pass / not_pass */
    private static String mapCheckResult(String result) {
        if (result == null || result.isBlank()) {
            throw new BusinessException("请填写核查结果");
        }
        return switch (result.trim()) {
            case "confirmed", "pass" -> "pass";
            case "not_found", "not_pass" -> "not_pass";
            case "unable" -> "unable";
            default -> throw new BusinessException("无效的核查结果");
        };
    }

    private void persistCheckAttachments(CheckTask task, List<String> attachments) {
        if (task == null || attachments == null || attachments.isEmpty()) {
            return;
        }
        Long uploaderId = task.getCollectorId() != null ? task.getCollectorId() : 1L;
        String uploaderName = task.getCollectorName() != null ? task.getCollectorName() : "采集员";
        for (String url : attachments) {
            if (url == null || url.isBlank()) {
                continue;
            }
            CheckAttachment att = new CheckAttachment();
            att.setCheckTaskId(task.getId());
            att.setFilePath(url.trim());
            att.setFileName(extractFileName(url));
            att.setFileType(guessFileType(url));
            att.setUploaderId(uploaderId);
            att.setUploaderName(uploaderName);
            checkAttachmentMapper.insert(att);
        }
    }

    private void persistVerifyAttachments(VerifyTask task, List<String> attachments) {
        if (task == null || attachments == null || attachments.isEmpty()) {
            return;
        }
        Long uploaderId = task.getCollectorId() != null ? task.getCollectorId() : 1L;
        String uploaderName = task.getCollectorName() != null ? task.getCollectorName() : "采集员";
        for (String url : attachments) {
            if (url == null || url.isBlank()) {
                continue;
            }
            VerifyAttachment att = new VerifyAttachment();
            att.setVerifyTaskId(task.getId());
            att.setFilePath(url.trim());
            att.setFileName(extractFileName(url));
            att.setFileType(guessFileType(url));
            att.setUploaderId(uploaderId);
            att.setUploaderName(uploaderName);
            verifyAttachmentMapper.insert(att);
        }
    }

    private static List<TaskAttachmentView> toAttachmentViews(List<?> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<TaskAttachmentView> list = new ArrayList<>();
        for (Object row : rows) {
            TaskAttachmentView v = new TaskAttachmentView();
            if (row instanceof CheckAttachment ca) {
                v.setId(ca.getId());
                v.setFilePath(ca.getFilePath());
                v.setFileName(ca.getFileName());
                v.setFileType(ca.getFileType());
            } else if (row instanceof VerifyAttachment va) {
                v.setId(va.getId());
                v.setFilePath(va.getFilePath());
                v.setFileName(va.getFileName());
                v.setFileType(va.getFileType());
            }
            list.add(v);
        }
        return list;
    }

    private static String extractFileName(String url) {
        String path = url.trim();
        int q = path.indexOf('?');
        if (q > 0) {
            path = path.substring(0, q);
        }
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String guessFileType(String url) {
        String lower = url.toLowerCase();
        if (lower.matches(".*\\.(mp4|mov|webm|avi)(\\?.*)?$")) {
            return "video";
        }
        return "image";
    }

    private static String labelCheckResult(String code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case "pass" -> "核查通过";
            case "not_pass" -> "核查不通过";
            case "unable" -> "无法核查";
            default -> code;
        };
    }

    private static String labelVerifyResult(String code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case "exist" -> "问题存在";
            case "not_exist" -> "问题不存在";
            case "unable" -> "无法核实";
            default -> code;
        };
    }

    /** 移动端：passed / not_passed → exist / not_exist */
    private static String mapVerifyResult(String result) {
        if (result == null || result.isBlank()) {
            throw new BusinessException("请填写核实结果");
        }
        return switch (result.trim()) {
            case "passed", "exist" -> "exist";
            case "not_passed", "not_exist" -> "not_exist";
            case "unable" -> "unable";
            default -> throw new BusinessException("无效的核实结果");
        };
    }
}
