package com.cityguard.appeal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.appeal.entity.AppealApply;
import com.cityguard.appeal.entity.AppealAttachment;

import java.util.List;
import java.util.Map;

public interface AppealService {

    AppealApply submitAppeal(Map<String, Object> appealData);

    AppealApply getAppealDetail(Long id);

    Page<AppealApply> getAppealList(Integer pageNum, Integer pageSize, Map<String, Object> params);

    List<AppealApply> getMyAppeals(Long applicantId);

    List<AppealApply> getPendingAppeals();

    AppealApply reviewAppeal(Long appealId, Integer result, String remark, Long reviewerId);

    List<AppealAttachment> getAppealAttachments(Long appealId);
}