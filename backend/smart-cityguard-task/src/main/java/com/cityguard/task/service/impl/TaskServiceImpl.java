package com.cityguard.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.task.entity.VerifyTask;
import com.cityguard.task.entity.CheckTask;
import com.cityguard.task.mapper.VerifyTaskMapper;
import com.cityguard.task.mapper.CheckTaskMapper;
import com.cityguard.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final VerifyTaskMapper verifyTaskMapper;
    private final CheckTaskMapper checkTaskMapper;

    @Override
    public VerifyTask getVerifyTaskDetail(Long id) {
        VerifyTask task = verifyTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("核查任务不存在");
        }
        return task;
    }

    @Override
    public CheckTask getCheckTaskDetail(Long id) {
        CheckTask task = checkTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("核实任务不存在");
        }
        return task;
    }

    @Override
    public Page<VerifyTask> getVerifyTaskList(Integer pageNum, Integer pageSize, Map<String, Object> params) {
        Page<VerifyTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<VerifyTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VerifyTask::getIsDeleted, 0);

        if (params.get("status") != null) {
            wrapper.eq(VerifyTask::getStatus, params.get("status"));
        }
        if (params.get("collectorId") != null) {
            wrapper.eq(VerifyTask::getCollectorId, params.get("collectorId"));
        }
        wrapper.orderByDesc(VerifyTask::getCreateTime);

        return verifyTaskMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<CheckTask> getCheckTaskList(Integer pageNum, Integer pageSize, Map<String, Object> params) {
        Page<CheckTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CheckTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckTask::getIsDeleted, 0);

        if (params.get("status") != null) {
            wrapper.eq(CheckTask::getStatus, params.get("status"));
        }
        if (params.get("collectorId") != null) {
            wrapper.eq(CheckTask::getCollectorId, params.get("collectorId"));
        }
        wrapper.orderByDesc(CheckTask::getCreateTime);

        return checkTaskMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public VerifyTask executeVerifyTask(Long taskId, Integer result, String remark, List<String> attachments) {
        VerifyTask task = getVerifyTaskDetail(taskId);
        if (task.getStatus() != 0) {
            throw new BusinessException("任务已完成，无法重复执行");
        }

        task.setStatus(1);
        task.setResult(result);
        task.setRemark(remark);
        task.setExecuteTime(LocalDateTime.now());
        verifyTaskMapper.updateById(task);

        return task;
    }

    @Override
    @Transactional
    public CheckTask executeCheckTask(Long taskId, Integer result, String remark, List<String> attachments) {
        CheckTask task = getCheckTaskDetail(taskId);
        if (task.getStatus() != 0) {
            throw new BusinessException("任务已完成，无法重复执行");
        }

        task.setStatus(1);
        task.setResult(result);
        task.setRemark(remark);
        task.setExecuteTime(LocalDateTime.now());
        checkTaskMapper.updateById(task);

        return task;
    }
}