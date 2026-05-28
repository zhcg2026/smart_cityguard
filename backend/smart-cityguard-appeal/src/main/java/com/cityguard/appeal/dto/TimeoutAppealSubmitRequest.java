package com.cityguard.appeal.dto;

import lombok.Data;

import java.util.List;

@Data
public class TimeoutAppealSubmitRequest {

    private Long caseId;
    private String appealDesc;
    /** 附件路径（/api/file 上传后路径） */
    private List<String> attachmentPaths;
}
