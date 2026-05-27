package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.util.List;

@Data
public class CaseDeleteRequest {

    /** 要删除的案件 ID 列表（支持单条） */
    private List<Long> caseIds;
}
