package com.cityguard.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cityguard.common.exception.BusinessException;
import com.cityguard.config.dto.CaseStandardSaveRequest;
import com.cityguard.config.dto.CategoryBigSaveRequest;
import com.cityguard.config.dto.CategorySmallSaveRequest;
import com.cityguard.config.entity.CaseStandard;
import com.cityguard.config.entity.CategoryBig;
import com.cityguard.config.entity.CategorySmall;
import com.cityguard.config.mapper.CaseStandardMapper;
import com.cityguard.config.mapper.CategoryBigMapper;
import com.cityguard.config.mapper.CategoryReferenceMapper;
import com.cityguard.config.mapper.CategorySmallMapper;
import com.cityguard.config.service.CategoryCatalogService;
import com.cityguard.config.util.CategoryCodeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryCatalogServiceImpl implements CategoryCatalogService {

    private final CategoryBigMapper categoryBigMapper;
    private final CategorySmallMapper categorySmallMapper;
    private final CaseStandardMapper caseStandardMapper;
    private final CategoryReferenceMapper categoryReferenceMapper;

    @Override
    public List<CategoryBig> listBigForManage(Integer type) {
        LambdaQueryWrapper<CategoryBig> w = new LambdaQueryWrapper<>();
        w.eq(CategoryBig::getDeleted, 0);
        String typeStr = mapApiType(type);
        if (typeStr != null) {
            w.eq(CategoryBig::getCategoryType, typeStr);
        }
        w.orderByAsc(CategoryBig::getSortOrder).orderByAsc(CategoryBig::getId);
        return categoryBigMapper.selectList(w);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryBig saveBig(CategoryBigSaveRequest request) {
        if (request == null) {
            throw new BusinessException("请求不能为空");
        }
        String categoryType = normalizeCategoryType(request.getCategoryType());
        String bigCode = CategoryCodeHelper.normalizeCode(request.getBigCode(), "大类编码");
        String bigName = trimRequired(request.getBigName(), "大类名称");

        if (request.getId() == null) {
            assertBigCodeUnique(categoryType, bigCode, null);
            CategoryBig big = new CategoryBig();
            big.setCategoryType(categoryType);
            big.setBigCode(bigCode);
            big.setBigName(bigName);
            big.setDescription(trimOptional(request.getDescription()));
            big.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : nextBigSort(categoryType));
            big.setStatus(request.getStatus() != null ? request.getStatus() : 1);
            big.setDeleted(0);
            categoryBigMapper.insert(big);
            return big;
        }

        CategoryBig existing = requireBig(request.getId());
        assertBigCodeUnique(categoryType, bigCode, existing.getId());
        existing.setBigName(bigName);
        existing.setDescription(trimOptional(request.getDescription()));
        if (request.getSortOrder() != null) {
            existing.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (!bigCode.equals(existing.getBigCode()) || !categoryType.equals(existing.getCategoryType())) {
            throw new BusinessException("已有大类不可修改编码或事部件类型，请新建或重新导入模板");
        }
        categoryBigMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBig(Long id) {
        CategoryBig big = requireBig(id);
        if (categoryReferenceMapper.countSmallsByBigId(id) > 0) {
            throw new BusinessException("该大类下仍有小类，请先删除或移走小类");
        }
        categoryBigMapper.deleteById(big.getId());
    }

    @Override
    public List<CategorySmall> listSmallForManage(Long bigId) {
        if (bigId == null) {
            return List.of();
        }
        LambdaQueryWrapper<CategorySmall> w = new LambdaQueryWrapper<>();
        w.eq(CategorySmall::getBigId, bigId).eq(CategorySmall::getDeleted, 0)
                .orderByAsc(CategorySmall::getSortOrder).orderByAsc(CategorySmall::getId);
        return categorySmallMapper.selectList(w);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategorySmall saveSmall(CategorySmallSaveRequest request) {
        if (request == null || request.getBigId() == null) {
            throw new BusinessException("请选择所属大类");
        }
        CategoryBig big = requireBig(request.getBigId());
        String smallCode = CategoryCodeHelper.normalizeCode(request.getSmallCode(), "小类编码");
        String smallName = trimRequired(request.getSmallName(), "小类名称");

        if (request.getId() == null) {
            assertSmallCodeUnique(big.getCategoryType(), big.getBigCode(), smallCode, null);
            CategorySmall sm = new CategorySmall();
            sm.setBigId(big.getId());
            sm.setBigCode(big.getBigCode());
            sm.setCategoryType(big.getCategoryType());
            sm.setSmallCode(smallCode);
            sm.setSmallName(smallName);
            sm.setFullCode(CategoryCodeHelper.buildFullCode(big.getBigCode(), smallCode));
            sm.setResponsibilitySubject(trimOptional(request.getResponsibilitySubject()));
            sm.setSuperviseSubject(trimOptional(request.getSuperviseSubject()));
            sm.setLegalBasis(trimOptional(request.getLegalBasis()));
            sm.setCollectRequirement(trimOptional(request.getCollectRequirement()));
            sm.setDescription(trimOptional(request.getDescription()));
            sm.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : nextSmallSort(big.getId()));
            sm.setStatus(request.getStatus() != null ? request.getStatus() : 1);
            sm.setIsExtended(0);
            sm.setDeleted(0);
            categorySmallMapper.insert(sm);
            return sm;
        }

        CategorySmall existing = requireSmall(request.getId());
        if (!existing.getBigId().equals(big.getId())) {
            throw new BusinessException("不可将小类迁移到其他大类，请新建小类");
        }
        assertSmallCodeUnique(big.getCategoryType(), big.getBigCode(), smallCode, existing.getId());
        existing.setSmallName(smallName);
        existing.setResponsibilitySubject(trimOptional(request.getResponsibilitySubject()));
        existing.setSuperviseSubject(trimOptional(request.getSuperviseSubject()));
        existing.setLegalBasis(trimOptional(request.getLegalBasis()));
        existing.setCollectRequirement(trimOptional(request.getCollectRequirement()));
        existing.setDescription(trimOptional(request.getDescription()));
        if (request.getSortOrder() != null) {
            existing.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (!smallCode.equals(existing.getSmallCode())) {
            throw new BusinessException("已有小类不可修改编码，请新建或重新导入模板");
        }
        categorySmallMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSmall(Long id) {
        requireSmall(id);
        if (categoryReferenceMapper.countCasesBySmallId(id) > 0) {
            throw new BusinessException("该小类已被案件引用，无法删除，可改为停用");
        }
        if (categoryReferenceMapper.countStandardsBySmallId(id) > 0) {
            throw new BusinessException("该小类下仍有立案条件，请先删除立案条件");
        }
        categorySmallMapper.deleteById(id);
    }

    @Override
    public List<CaseStandard> listStandardsForManage(Long smallId) {
        if (smallId == null) {
            return List.of();
        }
        LambdaQueryWrapper<CaseStandard> w = new LambdaQueryWrapper<>();
        w.eq(CaseStandard::getSmallId, smallId).eq(CaseStandard::getDeleted, 0)
                .orderByAsc(CaseStandard::getSortOrder).orderByAsc(CaseStandard::getId);
        return caseStandardMapper.selectList(w);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaseStandard saveStandard(CaseStandardSaveRequest request) {
        if (request == null || request.getSmallId() == null) {
            throw new BusinessException("请选择所属小类");
        }
        CategorySmall small = requireSmall(request.getSmallId());
        CategoryBig big = requireBig(small.getBigId());
        String conditionDesc = trimRequired(request.getConditionDesc(), "立案条件");
        String closeCondition = trimRequired(request.getCloseCondition(), "结案条件");
        String handleTimeType = trimRequired(request.getHandleTimeType(), "处置时限类型");
        int handleTimeValue = request.getHandleTimeValue() != null && request.getHandleTimeValue() > 0
                ? request.getHandleTimeValue() : 1;

        if (request.getId() == null) {
            int seq = nextStandardSeq(small.getId());
            CaseStandard cs = new CaseStandard();
            cs.setStandardCode(CategoryCodeHelper.buildStandardCode(small.getId(), seq));
            cs.setSmallId(small.getId());
            cs.setBigCode(big.getBigCode());
            cs.setSmallCode(small.getSmallCode());
            cs.setCategoryType(small.getCategoryType());
            cs.setConditionDesc(conditionDesc);
            cs.setCloseCondition(closeCondition);
            cs.setHandleTimeType(handleTimeType);
            cs.setHandleTimeValue(handleTimeValue);
            cs.setHandleTimeLimit(CategoryCodeHelper.formatHandleTimeLimit(handleTimeType, handleTimeValue));
            cs.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : seq);
            cs.setStatus(request.getStatus() != null ? request.getStatus() : 1);
            cs.setDeleted(0);
            caseStandardMapper.insert(cs);
            return cs;
        }

        CaseStandard existing = requireStandard(request.getId());
        if (!existing.getSmallId().equals(small.getId())) {
            throw new BusinessException("不可将立案条件迁移到其他小类");
        }
        existing.setConditionDesc(conditionDesc);
        existing.setCloseCondition(closeCondition);
        existing.setHandleTimeType(handleTimeType);
        existing.setHandleTimeValue(handleTimeValue);
        existing.setHandleTimeLimit(CategoryCodeHelper.formatHandleTimeLimit(handleTimeType, handleTimeValue));
        if (request.getSortOrder() != null) {
            existing.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        caseStandardMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStandard(Long id) {
        requireStandard(id);
        if (categoryReferenceMapper.countCasesByStandardId(id) > 0) {
            throw new BusinessException("该立案条件已被案件引用，无法删除，可改为停用");
        }
        caseStandardMapper.deleteById(id);
    }

    private static String mapApiType(Integer type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case 1 -> "component";
            case 2 -> "event";
            case 3 -> "service";
            default -> null;
        };
    }

    private static String normalizeCategoryType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("请选择事部件类型");
        }
        String t = raw.trim().toLowerCase();
        if ("component".equals(t) || "event".equals(t) || "service".equals(t)) {
            return t;
        }
        if ("1".equals(t) || "部件".equals(raw)) {
            return "component";
        }
        if ("2".equals(t) || "事件".equals(raw)) {
            return "event";
        }
        throw new BusinessException("无效的事部件类型");
    }

    private void assertBigCodeUnique(String categoryType, String bigCode, Long excludeId) {
        LambdaQueryWrapper<CategoryBig> w = new LambdaQueryWrapper<>();
        w.eq(CategoryBig::getCategoryType, categoryType)
                .eq(CategoryBig::getBigCode, bigCode)
                .eq(CategoryBig::getDeleted, 0);
        if (excludeId != null) {
            w.ne(CategoryBig::getId, excludeId);
        }
        if (categoryBigMapper.selectCount(w) > 0) {
            throw new BusinessException("该类型下大类编码已存在");
        }
    }

    private void assertSmallCodeUnique(String categoryType, String bigCode, String smallCode, Long excludeId) {
        LambdaQueryWrapper<CategorySmall> w = new LambdaQueryWrapper<>();
        w.eq(CategorySmall::getCategoryType, categoryType)
                .eq(CategorySmall::getBigCode, bigCode)
                .eq(CategorySmall::getSmallCode, smallCode)
                .eq(CategorySmall::getDeleted, 0);
        if (excludeId != null) {
            w.ne(CategorySmall::getId, excludeId);
        }
        if (categorySmallMapper.selectCount(w) > 0) {
            throw new BusinessException("该大类下小类编码已存在");
        }
    }

    private int nextBigSort(String categoryType) {
        LambdaQueryWrapper<CategoryBig> w = new LambdaQueryWrapper<>();
        w.eq(CategoryBig::getCategoryType, categoryType).eq(CategoryBig::getDeleted, 0)
                .orderByDesc(CategoryBig::getSortOrder).last("LIMIT 1");
        CategoryBig last = categoryBigMapper.selectOne(w);
        return last != null && last.getSortOrder() != null ? last.getSortOrder() + 1 : 1;
    }

    private int nextSmallSort(Long bigId) {
        LambdaQueryWrapper<CategorySmall> w = new LambdaQueryWrapper<>();
        w.eq(CategorySmall::getBigId, bigId).eq(CategorySmall::getDeleted, 0)
                .orderByDesc(CategorySmall::getSortOrder).last("LIMIT 1");
        CategorySmall last = categorySmallMapper.selectOne(w);
        return last != null && last.getSortOrder() != null ? last.getSortOrder() + 1 : 1;
    }

    private int nextStandardSeq(Long smallId) {
        return (int) categoryReferenceMapper.countStandardsBySmallId(smallId) + 1;
    }

    private CategoryBig requireBig(Long id) {
        CategoryBig big = categoryBigMapper.selectById(id);
        if (big == null || (big.getDeleted() != null && big.getDeleted() == 1)) {
            throw new BusinessException("大类不存在");
        }
        return big;
    }

    private CategorySmall requireSmall(Long id) {
        CategorySmall sm = categorySmallMapper.selectById(id);
        if (sm == null || (sm.getDeleted() != null && sm.getDeleted() == 1)) {
            throw new BusinessException("小类不存在");
        }
        return sm;
    }

    private CaseStandard requireStandard(Long id) {
        CaseStandard cs = caseStandardMapper.selectById(id);
        if (cs == null || (cs.getDeleted() != null && cs.getDeleted() == 1)) {
            throw new BusinessException("立案条件不存在");
        }
        return cs;
    }

    private static String trimRequired(String s, String label) {
        if (s == null || s.isBlank()) {
            throw new BusinessException(label + "不能为空");
        }
        return s.trim();
    }

    private static String trimOptional(String s) {
        return s != null ? s.trim() : null;
    }
}
