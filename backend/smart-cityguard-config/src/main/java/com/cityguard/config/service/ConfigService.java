package com.cityguard.config.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.config.entity.CategoryBig;
import com.cityguard.config.entity.CategorySmall;
import com.cityguard.config.entity.CaseStandard;
import com.cityguard.config.entity.TimeLimitRule;

import java.util.List;

public interface ConfigService {

    List<CategoryBig> getCategoryBigList(Integer type);

    List<CategorySmall> getCategorySmallList(Long categoryBigId);

    List<CaseStandard> getConditions(Long categorySmallId);

    TimeLimitRule getTimeLimitRule(Long categorySmallId);
}