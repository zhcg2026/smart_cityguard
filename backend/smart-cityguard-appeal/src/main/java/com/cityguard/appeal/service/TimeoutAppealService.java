package com.cityguard.appeal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.appeal.dto.TimeoutAppealDetailVo;
import com.cityguard.appeal.dto.TimeoutAppealReviewRequest;
import com.cityguard.appeal.dto.TimeoutAppealSubmitRequest;
import com.cityguard.appeal.entity.AppealApply;
import com.cityguard.auth.entity.LoginUser;
import com.cityguard.caseinfo.entity.CaseInfo;

import java.util.List;

public interface TimeoutAppealService {

    AppealApply submit(TimeoutAppealSubmitRequest request, LoginUser user);

    AppealApply deptReview(TimeoutAppealReviewRequest request, LoginUser user);

    AppealApply dispatcherReview(TimeoutAppealReviewRequest request, LoginUser user);

    AppealApply acceptorReview(TimeoutAppealReviewRequest request, LoginUser user);

    TimeoutAppealDetailVo getDetail(Long appealId, LoginUser user);

    AppealApply getByCaseId(Long caseId);

    Page<AppealApply> list(Integer pageNum, Integer pageSize, String tab, String caseCode, LoginUser user);

    Page<CaseInfo> listAppealableCases(Integer pageNum, Integer pageSize, String caseCode, LoginUser user);
}
