package com.cityguard.config.service;

import com.cityguard.config.dto.CaseStandardSaveRequest;
import com.cityguard.config.dto.CategoryBigSaveRequest;
import com.cityguard.config.dto.CategorySmallSaveRequest;
import com.cityguard.config.entity.CaseStandard;
import com.cityguard.config.entity.CategoryBig;
import com.cityguard.config.entity.CategorySmall;

import java.util.List;

public interface CategoryCatalogService {

    List<CategoryBig> listBigForManage(Integer type);

    CategoryBig saveBig(CategoryBigSaveRequest request);

    void deleteBig(Long id);

    List<CategorySmall> listSmallForManage(Long bigId);

    CategorySmall saveSmall(CategorySmallSaveRequest request);

    void deleteSmall(Long id);

    List<CaseStandard> listStandardsForManage(Long smallId);

    CaseStandard saveStandard(CaseStandardSaveRequest request);

    void deleteStandard(Long id);
}
