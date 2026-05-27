package com.cityguard.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cityguard.common.enums.TimeLimitTypeEnum;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.config.dto.SmallTimeLimitRowVO;
import com.cityguard.config.dto.TimeLimitOverrideSaveRequest;
import com.cityguard.config.entity.CategoryBig;
import com.cityguard.config.entity.CategorySmall;
import com.cityguard.config.entity.CaseStandard;
import com.cityguard.config.entity.CategoryTimeLimitOverride;
import com.cityguard.config.entity.TimeLimitRule;
import com.cityguard.config.mapper.CategoryBigMapper;
import com.cityguard.config.mapper.CategorySmallMapper;
import com.cityguard.config.mapper.CaseStandardMapper;
import com.cityguard.config.mapper.CategoryTimeLimitOverrideMapper;
import com.cityguard.config.mapper.TimeLimitRuleMapper;
import com.cityguard.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private final CategoryBigMapper categoryBigMapper;
    private final CategorySmallMapper categorySmallMapper;
    private final CaseStandardMapper caseStandardMapper;
    private final TimeLimitRuleMapper timeLimitRuleMapper;
    private final CategoryTimeLimitOverrideMapper categoryTimeLimitOverrideMapper;

    @Override
    public List<CategoryBig> getCategoryBigList(Integer type) {
        if (type != null) {
            String typeStr = switch (type) {
                case 1 -> "component";
                case 2 -> "event";
                case 3 -> "service";
                default -> null;
            };
            if (typeStr != null) {
                return categoryBigMapper.selectByType(typeStr);
            }
        }
        return categoryBigMapper.selectAllActive();
    }

    @Override
    public List<CategorySmall> getCategorySmallList(Long categoryBigId) {
        return categorySmallMapper.selectByCategoryBigId(categoryBigId);
    }

    @Override
    public List<CaseStandard> getConditions(Long categorySmallId) {
        return caseStandardMapper.selectList(
                new LambdaQueryWrapper<CaseStandard>()
                        .eq(CaseStandard::getSmallId, categorySmallId)
                        .eq(CaseStandard::getStatus, 1)
                        .orderByAsc(CaseStandard::getSortOrder));
    }

    @Override
    public TimeLimitRule getTimeLimitRule(Long categorySmallId) {
        CategoryTimeLimitOverride override = categoryTimeLimitOverrideMapper.selectBySmallId(categorySmallId);
        if (override == null) {
            return null;
        }
        TimeLimitRule rule = timeLimitRuleMapper.selectByType(override.getTimeLimitType());
        if (rule == null) {
            rule = new TimeLimitRule();
            rule.setTimeLimitType(override.getTimeLimitType());
            rule.setTypeName(typeName(override.getTimeLimitType()));
        }
        return rule;
    }

    @Override
    public List<TimeLimitRule> listTimeLimitRules() {
        return timeLimitRuleMapper.selectAllActive();
    }

    @Override
    public List<SmallTimeLimitRowVO> listSmallTimeLimits(Long bigId) {
        if (bigId == null) {
            return List.of();
        }
        CategoryBig big = categoryBigMapper.selectById(bigId);
        String bigName = big != null ? big.getBigName() : null;
        String categoryType = big != null ? big.getCategoryType() : null;

        List<CategorySmall> smalls = categorySmallMapper.selectByCategoryBigId(bigId);
        List<SmallTimeLimitRowVO> rows = new ArrayList<>();
        for (CategorySmall small : smalls) {
            SmallTimeLimitRowVO row = new SmallTimeLimitRowVO();
            row.setSmallId(small.getId());
            row.setSmallCode(small.getSmallCode());
            row.setSmallName(small.getSmallName());
            row.setBigId(bigId);
            row.setBigName(bigName);
            row.setCategoryType(categoryType);

            CaseStandard firstStandard = resolveFirstStandard(small.getId());
            if (firstStandard != null && StringUtils.hasText(firstStandard.getHandleTimeType())) {
                row.setDefaultTimeLimitType(firstStandard.getHandleTimeType());
                row.setDefaultTimeLimitTypeName(typeName(firstStandard.getHandleTimeType()));
                row.setDefaultTimeLimitValue(firstStandard.getHandleTimeValue());
            }

            CategoryTimeLimitOverride override = categoryTimeLimitOverrideMapper.selectBySmallId(small.getId());
            if (override != null) {
                row.setOverridden(true);
                row.setOverrideId(override.getId());
                row.setOverrideTimeLimitType(override.getTimeLimitType());
                row.setOverrideTimeLimitTypeName(typeName(override.getTimeLimitType()));
                row.setOverrideTimeLimitValue(override.getTimeLimitValue());
                row.setOverrideRemark(override.getRemark());
                row.setEffectiveTimeLimitType(override.getTimeLimitType());
                row.setEffectiveTimeLimitTypeName(typeName(override.getTimeLimitType()));
                row.setEffectiveTimeLimitValue(override.getTimeLimitValue());
            } else if (row.getDefaultTimeLimitType() != null) {
                row.setEffectiveTimeLimitType(row.getDefaultTimeLimitType());
                row.setEffectiveTimeLimitTypeName(row.getDefaultTimeLimitTypeName());
                row.setEffectiveTimeLimitValue(row.getDefaultTimeLimitValue());
            } else {
                row.setEffectiveTimeLimitType("work_hour");
                row.setEffectiveTimeLimitTypeName(typeName("work_hour"));
                row.setEffectiveTimeLimitValue(4);
            }
            rows.add(row);
        }
        return rows;
    }

    @Override
    @Transactional
    public CategoryTimeLimitOverride saveTimeLimitOverride(TimeLimitOverrideSaveRequest request) {
        if (request.getSmallId() == null) {
            throw new BusinessException("请选择小类");
        }
        if (!StringUtils.hasText(request.getTimeLimitType()) || request.getTimeLimitValue() == null) {
            throw new BusinessException("请填写时限类型与数值");
        }
        if (request.getTimeLimitValue() <= 0) {
            throw new BusinessException("时限数值须大于 0");
        }
        if (TimeLimitTypeEnum.fromCode(request.getTimeLimitType()) == null) {
            throw new BusinessException("无效的时限类型");
        }
        CategorySmall small = categorySmallMapper.selectById(request.getSmallId());
        if (small == null) {
            throw new BusinessException("小类不存在");
        }

        CategoryTimeLimitOverride entity;
        if (request.getId() != null) {
            entity = categoryTimeLimitOverrideMapper.selectById(request.getId());
            if (entity == null) {
                throw new BusinessException("覆盖配置不存在");
            }
        } else {
            CategoryTimeLimitOverride existing = categoryTimeLimitOverrideMapper.selectBySmallId(request.getSmallId());
            if (existing != null) {
                entity = existing;
            } else {
                entity = new CategoryTimeLimitOverride();
                entity.setSmallId(request.getSmallId());
                entity.setStatus(1);
            }
        }
        entity.setTimeLimitType(request.getTimeLimitType());
        entity.setTimeLimitValue(request.getTimeLimitValue());
        entity.setRemark(request.getRemark());
        entity.setStatus(1);

        if (entity.getId() == null) {
            categoryTimeLimitOverrideMapper.insert(entity);
        } else {
            categoryTimeLimitOverrideMapper.updateById(entity);
        }
        return entity;
    }

    @Override
    @Transactional
    public void deleteTimeLimitOverride(Long id) {
        if (id == null) {
            throw new BusinessException("ID 不能为空");
        }
        categoryTimeLimitOverrideMapper.deleteById(id);
    }

    private CaseStandard resolveFirstStandard(Long smallId) {
        List<CaseStandard> list = getConditions(smallId);
        return list.isEmpty() ? null : list.get(0);
    }

    private static String typeName(String code) {
        TimeLimitTypeEnum e = TimeLimitTypeEnum.fromCode(code);
        return e != null ? e.getName() : code;
    }
}
