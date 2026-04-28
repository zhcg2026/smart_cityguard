package com.cityguard.caseinfo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("case_flow_record")
public class CaseFlowRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long caseId;

    private Integer operateType;

    private String operateName;

    private Long operatorId;

    private String operatorName;

    private String remark;

    private LocalDateTime operateTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}