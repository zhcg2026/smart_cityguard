package com.cityguard.task.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.task.dto.CheckTaskRecordView;
import com.cityguard.task.dto.VerifyTaskRecordView;
import com.cityguard.task.entity.CheckTask;
import com.cityguard.task.entity.VerifyTask;

import java.util.List;
import java.util.Map;

public interface TaskService {

    VerifyTask getVerifyTaskDetail(Long id);

    CheckTask getCheckTaskDetail(Long id);

    Page<VerifyTask> getVerifyTaskList(Integer pageNum, Integer pageSize, Map<String, Object> params);

    Page<CheckTask> getCheckTaskList(Integer pageNum, Integer pageSize, Map<String, Object> params);

    CheckTask executeCheckTask(Long taskId, String result, String remark, List<String> attachments, Long collectorId);

    VerifyTask executeVerifyTask(Long taskId, String result, String remark, List<String> attachments, Long collectorId);

    boolean hasPendingCheckTask(Long caseId);

    boolean hasPendingVerifyTask(Long caseId);

    List<CheckTaskRecordView> listCheckTaskRecordsByCaseId(Long caseId);

    List<VerifyTaskRecordView> listVerifyTaskRecordsByCaseId(Long caseId);
}
