package com.cityguard.caseinfo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.caseinfo.entity.*;
import com.cityguard.caseinfo.mapper.*;
import com.cityguard.caseinfo.service.CaseService;
import com.cityguard.common.constant.CaseStatusConstant;
import com.cityguard.common.enums.CaseStatusEnum;
import com.cityguard.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CaseServiceImpl implements CaseService {

    private final CaseInfoMapper caseInfoMapper;
    private final CaseFlowRecordMapper flowRecordMapper;
    private final CaseAttachmentMapper attachmentMapper;

    @Override
    @Transactional
    public CaseInfo reportCase(Map<String, Object> reportData) {
        CaseInfo caseInfo = new CaseInfo();
        caseInfo.setCaseNo(generateCaseNo());
        caseInfo.setCaseSource(CaseStatusConstant.SOURCE_COLLECTOR_APP);
        caseInfo.setCategoryBigId(Long.valueOf(reportData.get("categoryBigId").toString()));
        caseInfo.setCategoryBigName((String) reportData.get("categoryBigName"));
        caseInfo.setCategorySmallId(Long.valueOf(reportData.get("categorySmallId").toString()));
        caseInfo.setCategorySmallName((String) reportData.get("categorySmallName"));
        caseInfo.setConditionId(Long.valueOf(reportData.get("conditionId").toString()));
        caseInfo.setConditionName((String) reportData.get("conditionName"));
        caseInfo.setAddress((String) reportData.get("address"));
        caseInfo.setLongitude(Double.valueOf(reportData.get("longitude").toString()));
        caseInfo.setLatitude(Double.valueOf(reportData.get("latitude").toString()));
        caseInfo.setDescription((String) reportData.get("description"));
        caseInfo.setStatus(CaseStatusEnum.PENDING_VERIFY.getCode());
        caseInfo.setReportTime(LocalDateTime.now());
        caseInfo.setCreateTime(LocalDateTime.now());
        caseInfo.setIsOverdue(0);

        caseInfoMapper.insert(caseInfo);

        // 保存附件
        List<String> attachmentUrls = (List<String>) reportData.get("attachments");
        if (attachmentUrls != null && !attachmentUrls.isEmpty()) {
            for (String url : attachmentUrls) {
                CaseAttachment attachment = new CaseAttachment();
                attachment.setCaseId(caseInfo.getId());
                attachment.setAttachmentType(1);
                attachment.setFileUrl(url);
                attachment.setFlowType(1);
                attachment.setUploadTime(LocalDateTime.now());
                attachmentMapper.insert(attachment);
            }
        }

        // 记录流程
        saveFlowRecord(caseInfo.getId(), CaseStatusEnum.PENDING_VERIFY.getCode(), "问题上报", "采集员上报问题");

        return caseInfo;
    }

    @Override
    public CaseInfo getCaseDetail(Long id) {
        CaseInfo caseInfo = caseInfoMapper.selectById(id);
        if (caseInfo == null) {
            throw new BusinessException("案件不存在");
        }
        return caseInfo;
    }

    @Override
    public Page<CaseInfo> getCaseList(Integer pageNum, Integer pageSize, Map<String, Object> params) {
        Page<CaseInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CaseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CaseInfo::getIsDeleted, 0);

        if (params.get("caseNo") != null) {
            wrapper.like(CaseInfo::getCaseNo, params.get("caseNo"));
        }
        if (params.get("status") != null) {
            wrapper.eq(CaseInfo::getStatus, params.get("status"));
        }
        if (params.get("categoryBigId") != null) {
            wrapper.eq(CaseInfo::getCategoryBigId, params.get("categoryBigId"));
        }
        wrapper.orderByDesc(CaseInfo::getReportTime);

        return caseInfoMapper.selectPage(page, wrapper);
    }

    @Override
    public List<CaseInfo> getPendingCases(Integer status) {
        return caseInfoMapper.selectByStatus(status);
    }

    @Override
    @Transactional
    public CaseInfo registerCase(Long caseId, String remark) {
        CaseInfo caseInfo = getCaseDetail(caseId);
        if (caseInfo.getStatus() != CaseStatusEnum.PENDING_REGISTER.getCode()) {
            throw new BusinessException("案件状态不正确，无法立案");
        }

        caseInfo.setStatus(CaseStatusEnum.PENDING_DISPATCH.getCode());
        caseInfo.setRegisterTime(LocalDateTime.now());
        caseInfoMapper.updateById(caseInfo);

        saveFlowRecord(caseId, CaseStatusEnum.PENDING_DISPATCH.getCode(), "立案", remark);

        return caseInfo;
    }

    @Override
    @Transactional
    public CaseInfo dispatchCase(Long caseId, Long departmentId, String remark) {
        CaseInfo caseInfo = getCaseDetail(caseId);
        if (caseInfo.getStatus() != CaseStatusEnum.PENDING_DISPATCH.getCode()) {
            throw new BusinessException("案件状态不正确，无法派遣");
        }

        caseInfo.setStatus(CaseStatusEnum.PENDING_HANDLE.getCode());
        caseInfo.setDepartmentId(departmentId);
        caseInfo.setDispatchTime(LocalDateTime.now());
        caseInfoMapper.updateById(caseInfo);

        saveFlowRecord(caseId, CaseStatusEnum.PENDING_HANDLE.getCode(), "派遣", remark);

        return caseInfo;
    }

    @Override
    @Transactional
    public CaseInfo handleCase(Long caseId, String remark, List<String> attachments) {
        CaseInfo caseInfo = getCaseDetail(caseId);
        if (caseInfo.getStatus() != CaseStatusEnum.PENDING_HANDLE.getCode()) {
            throw new BusinessException("案件状态不正确，无法处置");
        }

        caseInfo.setStatus(CaseStatusEnum.PENDING_CHECK.getCode());
        caseInfo.setHandleTime(LocalDateTime.now());
        caseInfoMapper.updateById(caseInfo);

        // 保存处置附件
        if (attachments != null && !attachments.isEmpty()) {
            for (String url : attachments) {
                CaseAttachment attachment = new CaseAttachment();
                attachment.setCaseId(caseId);
                attachment.setAttachmentType(1);
                attachment.setFileUrl(url);
                attachment.setFlowType(4);
                attachment.setUploadTime(LocalDateTime.now());
                attachmentMapper.insert(attachment);
            }
        }

        saveFlowRecord(caseId, CaseStatusEnum.PENDING_CHECK.getCode(), "处置", remark);

        return caseInfo;
    }

    @Override
    @Transactional
    public CaseInfo verifyCase(Long caseId, Integer result, String remark, List<String> attachments) {
        CaseInfo caseInfo = getCaseDetail(caseId);
        if (caseInfo.getStatus() != CaseStatusEnum.PENDING_VERIFY.getCode()) {
            throw new BusinessException("案件状态不正确，无法核查");
        }

        if (result == 1) {
            caseInfo.setStatus(CaseStatusEnum.PENDING_REGISTER.getCode());
        } else {
            caseInfo.setStatus(CaseStatusEnum.REJECTED.getCode());
            caseInfo.setRejectReason(remark);
        }
        caseInfoMapper.updateById(caseInfo);

        saveFlowRecord(caseId, caseInfo.getStatus(), "核查", remark);

        return caseInfo;
    }

    @Override
    @Transactional
    public CaseInfo checkCase(Long caseId, Integer result, String remark, List<String> attachments) {
        CaseInfo caseInfo = getCaseDetail(caseId);
        if (caseInfo.getStatus() != CaseStatusEnum.PENDING_CHECK.getCode()) {
            throw new BusinessException("案件状态不正确，无法核实");
        }

        if (result == 1) {
            caseInfo.setStatus(CaseStatusEnum.CLOSED.getCode());
            caseInfo.setCloseTime(LocalDateTime.now());
        } else {
            caseInfo.setStatus(CaseStatusEnum.PENDING_HANDLE.getCode());
        }
        caseInfoMapper.updateById(caseInfo);

        saveFlowRecord(caseId, caseInfo.getStatus(), "核实", remark);

        return caseInfo;
    }

    @Override
    @Transactional
    public CaseInfo closeCase(Long caseId, String remark) {
        CaseInfo caseInfo = getCaseDetail(caseId);
        caseInfo.setStatus(CaseStatusEnum.CLOSED.getCode());
        caseInfo.setCloseTime(LocalDateTime.now());
        caseInfoMapper.updateById(caseInfo);

        saveFlowRecord(caseId, CaseStatusEnum.CLOSED.getCode(), "结案", remark);

        return caseInfo;
    }

    @Override
    @Transactional
    public CaseInfo rejectCase(Long caseId, String reason) {
        CaseInfo caseInfo = getCaseDetail(caseId);
        if (!caseInfo.getStatus().equals(CaseStatusEnum.PENDING_VERIFY.getCode()) &&
            !caseInfo.getStatus().equals(CaseStatusEnum.PENDING_REGISTER.getCode())) {
            throw new BusinessException("案件状态不正确，无法不受理");
        }

        caseInfo.setStatus(CaseStatusEnum.REJECTED.getCode());
        caseInfo.setRejectReason(reason);
        caseInfoMapper.updateById(caseInfo);

        saveFlowRecord(caseId, CaseStatusEnum.REJECTED.getCode(), "不受理", reason);

        return caseInfo;
    }

    @Override
    public List<CaseFlowRecord> getFlowRecords(Long caseId) {
        return flowRecordMapper.selectByCaseId(caseId);
    }

    @Override
    public List<CaseAttachment> getAttachments(Long caseId) {
        return attachmentMapper.selectByCaseId(caseId);
    }

    private String generateCaseNo() {
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "YC" + dateStr;
        LambdaQueryWrapper<CaseInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(CaseInfo::getCaseNo, prefix)
               .orderByDesc(CaseInfo::getCaseNo)
               .last("LIMIT 1");
        CaseInfo lastCase = caseInfoMapper.selectOne(wrapper);

        int seq = 1;
        if (lastCase != null && lastCase.getCaseNo() != null) {
            String lastNo = lastCase.getCaseNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + String.format("%04d", seq);
    }

    private void saveFlowRecord(Long caseId, Integer operateType, String operateName, String remark) {
        CaseFlowRecord record = new CaseFlowRecord();
        record.setCaseId(caseId);
        record.setOperateType(operateType);
        record.setOperateName(operateName);
        record.setOperatorId(1L);
        record.setOperatorName("系统");
        record.setRemark(remark);
        record.setOperateTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        flowRecordMapper.insert(record);
    }
}