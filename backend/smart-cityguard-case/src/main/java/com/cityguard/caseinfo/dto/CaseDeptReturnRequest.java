package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处置部门认为非本部门职责，回退至派遣员
 */
@Data
public class CaseDeptReturnRequest {

    private Long caseId;

    private String remark;

    private LocalDateTime clientUpdateTime;
}
