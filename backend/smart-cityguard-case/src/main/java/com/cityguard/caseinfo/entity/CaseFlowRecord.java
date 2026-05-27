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

    /** 与主表 case_info.case_code 一致；插入由 CaseServiceImpl 通过 JdbcTemplate 完成。 */
    private String caseCode;

    // 流转节点
    private String nodeCode;
    private String nodeName;

    // 操作信息
    private String operateType;
    private String operateResult;
    private String operateOpinion;

    // 操作人信息
    private Long operatorId;
    private String operatorName;
    private String operatorPhone;
    private Long operatorDeptId;
    private String operatorDeptName;
    private String operatorPosition;

    // 接收人信息
    private Long receiverId;
    private String receiverName;
    private Long receiverDeptId;
    private String receiverDeptName;

    // 时间信息
    private LocalDateTime operateTime;
    private LocalDateTime receiveTime;
    private Integer timeUsed;
    private Integer isTimeout;

    // 附件
    private String attachments;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}