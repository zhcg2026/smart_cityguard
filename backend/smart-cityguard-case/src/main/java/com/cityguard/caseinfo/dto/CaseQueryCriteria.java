package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.util.List;

/**
 * 综合查询条件（与报表统计共用，见 docs/case-comprehensive-query-design.md）
 */
@Data
public class CaseQueryCriteria {

    /** 案件编号 */
    private String caseCode;

    /** 编号匹配：exact（精确）、prefix（前缀） */
    private String caseCodeMatch;

    private CaseDateFilter reportTime;
    private CaseDateFilter closeTime;

    /** 问题来源 source_type，多选 */
    private List<String> sourceTypes;

    /** 责任网格 resp_grid_id，多选 */
    private List<Long> respGridIds;

    /** 问题状态 case_status，多选 */
    private List<String> caseStatuses;

    /** 问题小类 small_id，多选 */
    private List<Long> smallIds;

    private Long handleDeptId;
    private Long reporterId;
    private Long registerOperatorId;
    private Long dispatchOperatorId;

    /** 地址描述 */
    private String address;

    /** 地址匹配：eq、contains */
    private String addressMatch;

    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
