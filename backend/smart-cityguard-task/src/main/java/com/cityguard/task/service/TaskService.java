package com.cityguard.task.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.task.entity.VerifyTask;
import com.cityguard.task.entity.CheckTask;

import java.util.List;
import java.util.Map;

public interface TaskService {

    VerifyTask getVerifyTaskDetail(Long id);

    CheckTask getCheckTaskDetail(Long id);

    Page<VerifyTask> getVerifyTaskList(Integer pageNum, Integer pageSize, Map<String, Object> params);

    Page<CheckTask> getCheckTaskList(Integer pageNum, Integer pageSize, Map<String, Object> params);

    VerifyTask executeVerifyTask(Long taskId, Integer result, String remark, List<String> attachments);

    CheckTask executeCheckTask(Long taskId, Integer result, String remark, List<String> attachments);
}