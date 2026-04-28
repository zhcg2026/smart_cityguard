package com.cityguard.config.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.config.entity.CategoryBig;
import com.cityguard.config.entity.CategorySmall;
import com.cityguard.config.entity.CaseStandard;
import com.cityguard.config.entity.TimeLimitRule;
import com.cityguard.config.mapper.CategoryBigMapper;
import com.cityguard.config.mapper.CategorySmallMapper;
import com.cityguard.config.mapper.CaseStandardMapper;
import com.cityguard.config.mapper.TimeLimitRuleMapper;
import com.cityguard.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private final CategoryBigMapper categoryBigMapper;
    private final CategorySmallMapper categorySmallMapper;
    private final CaseStandardMapper caseStandardMapper;
    private final TimeLimitRuleMapper timeLimitRuleMapper;

    @Override
    public List<CategoryBig> getCategoryBigList(Integer type) {
        if (type != null) {
            return categoryBigMapper.selectByType(type);
        }
        return categoryBigMapper.selectAllActive();
    }

    @Override
    public List<CategorySmall> getCategorySmallList(Long categoryBigId) {
        return categorySmallMapper.selectByCategoryBigId(categoryBigId);
    }

    @Override
    public List<CaseStandard> getConditions(Long categorySmallId) {
        return caseStandardMapper.selectByCategorySmallId(categorySmallId);
    }

    @Override
    public TimeLimitRule getTimeLimitRule(Long categorySmallId) {
        return timeLimitRuleMapper.selectByCategorySmallId(categorySmallId);
    }
}