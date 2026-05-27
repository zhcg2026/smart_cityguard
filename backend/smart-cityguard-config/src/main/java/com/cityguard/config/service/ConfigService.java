package com.cityguard.config.service;

import com.cityguard.config.dto.SmallTimeLimitRowVO;
import com.cityguard.config.dto.TimeLimitOverrideSaveRequest;
import com.cityguard.config.entity.CategoryBig;
import com.cityguard.config.entity.CategorySmall;
import com.cityguard.config.entity.CaseStandard;
import com.cityguard.config.entity.CategoryTimeLimitOverride;
import com.cityguard.config.entity.TimeLimitRule;

import java.util.List;

public interface ConfigService {

    List<CategoryBig> getCategoryBigList(Integer type);

    List<CategorySmall> getCategorySmallList(Long categoryBigId);

    List<CaseStandard> getConditions(Long categorySmallId);

    TimeLimitRule getTimeLimitRule(Long categorySmallId);

    List<TimeLimitRule> listTimeLimitRules();

    List<SmallTimeLimitRowVO> listSmallTimeLimits(Long bigId);

    CategoryTimeLimitOverride saveTimeLimitOverride(TimeLimitOverrideSaveRequest request);

    void deleteTimeLimitOverride(Long id);
}