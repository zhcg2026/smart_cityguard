package com.cityguard.caseinfo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 处置部门对当前处置人员当次反馈（指派/打回/驳回延期挂账等） */
@Data
public class HandlerDeptNotice {
    private String title;
    private String content;
    private LocalDateTime time;
}
