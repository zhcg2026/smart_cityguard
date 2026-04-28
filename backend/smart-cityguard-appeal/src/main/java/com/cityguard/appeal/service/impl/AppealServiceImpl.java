package com.cityguard.appeal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.appeal.entity.AppealApply;
import com.cityguard.appeal.entity.AppealAttachment;
import com.cityguard.appeal.mapper.AppealApplyMapper;
import com.cityguard.appeal.mapper.AppealAttachmentMapper;
import com.cityguard.appeal.service.AppealService;
import com.cityguard.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppealServiceImpl implements AppealService {

    private final AppealApplyMapper appealApplyMapper;
    private final AppealAttachmentMapper appealAttachmentMapper;

    @Override
    @Transactional
    public AppealApply submitAppeal(Map<String, Object> appealData) {
        AppealApply appeal = new AppealApply();
        appeal.setAppealType((String) appealData.get("appealType"));
        appeal.setCaseId(Long.valueOf(appealData.get("caseId").toString()));
        appeal.setCaseNo((String) appealData.get("caseNo"));
        appeal.setApplicantId(Long.valueOf(appealData.get("applicantId").toString()));
        appeal.setApplicantName((String) appealData.get("applicantName"));
        appeal.setReason((String) appealData.get("reason"));
        appeal.setStatus(0);
        appeal.setCreateTime(LocalDateTime.now());

        appealApplyMapper.insert(appeal);

        List<String> attachments = (List<String>) appealData.get("attachments");
        if (attachments != null && !attachments.isEmpty()) {
            for (String url : attachments) {
                AppealAttachment attachment = new AppealAttachment();
                attachment.setAppealId(appeal.getId());
                attachment.setFileUrl(url);
                attachment.setUploadTime(LocalDateTime.now());
                appealAttachmentMapper.insert(attachment);
            }
        }

        return appeal;
    }

    @Override
    public AppealApply getAppealDetail(Long id) {
        AppealApply appeal = appealApplyMapper.selectById(id);
        if (appeal == null) {
            throw new BusinessException("申诉不存在");
        }
        return appeal;
    }

    @Override
    public Page<AppealApply> getAppealList(Integer pageNum, Integer pageSize, Map<String, Object> params) {
        Page<AppealApply> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AppealApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppealApply::getIsDeleted, 0);

        if (params.get("status") != null) {
            wrapper.eq(AppealApply::getStatus, params.get("status"));
        }
        if (params.get("applicantId") != null) {
            wrapper.eq(AppealApply::getApplicantId, params.get("applicantId"));
        }
        wrapper.orderByDesc(AppealApply::getCreateTime);

        return appealApplyMapper.selectPage(page, wrapper);
    }

    @Override
    public List<AppealApply> getMyAppeals(Long applicantId) {
        return appealApplyMapper.selectByApplicantId(applicantId);
    }

    @Override
    public List<AppealApply> getPendingAppeals() {
        return appealApplyMapper.selectByStatus(0);
    }

    @Override
    @Transactional
    public AppealApply reviewAppeal(Long appealId, Integer result, String remark, Long reviewerId) {
        AppealApply appeal = getAppealDetail(appealId);
        if (appeal.getStatus() != 0) {
            throw new BusinessException("申诉已审核，无法重复审核");
        }

        appeal.setStatus(result);
        appeal.setReviewResult(result == 1 ? "通过" : "驳回");
        appeal.setReviewRemark(remark);
        appeal.setReviewTime(LocalDateTime.now());
        appeal.setReviewerId(reviewerId);
        appealApplyMapper.updateById(appeal);

        return appeal;
    }

    @Override
    public List<AppealAttachment> getAppealAttachments(Long appealId) {
        return appealAttachmentMapper.selectByAppealId(appealId);
    }
}