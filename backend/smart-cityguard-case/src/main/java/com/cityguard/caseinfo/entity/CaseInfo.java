package com.cityguard.caseinfo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("case_info")
public class CaseInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 案件编码
    private String caseCode;

    // 问题分类
    private String categoryType;
    private String bigCode;
    private String bigName;
    private String smallCode;
    private String smallName;
    private Long smallId;
    private Long standardId;
    private String conditionDesc;

    // 来源信息
    private String sourceType;
    private String sourceDesc;

    // 上报人信息
    private Long reporterId;
    private String reporterName;
    private String reporterPhone;

    // 位置信息
    private Double longitude;
    private Double latitude;
    private String address;
    private Long streetId;
    private Long communityId;
    private Long gridId;
    private Long respGridId;

    /** 详情展示：责任片区名称（非表字段） */
    @TableField(exist = false)
    private String respGridName;

    /** 详情展示：单元网格名称（非表字段） */
    @TableField(exist = false)
    private String gridName;

    // 案件描述
    private String description;
    private String remark;

    // 案件状态
    private String caseStatus;
    private String currentNode;
    private Long currentHandlerId;
    private String currentHandlerName;
    private Long currentDeptId;
    private String currentDeptName;

    // 处置部门信息
    private Long handleDeptId;
    private String handleDeptName;

    // 时限信息
    private String timeLimitType;
    private Integer timeLimitValue;
    private LocalDateTime deadlineTime;

    // 时间节点
    private LocalDateTime reportTime;
    private LocalDateTime acceptTime;
    /** 立案受理员（归属人，全程可查；核实/结案以 current_handler 批转为准） */
    private Long registerOperatorId;
    private String registerOperatorName;
    private LocalDateTime dispatchTime;
    /** 派遣至处置部门的操作员（当案派遣员） */
    private Long dispatchOperatorId;
    private String dispatchOperatorName;
    private LocalDateTime handleReceiveTime;
    private LocalDateTime handleFinishTime;
    private LocalDateTime checkTime;
    private LocalDateTime closeTime;

    // 标记
    private Integer isUrgent;
    private Integer isSupervised;
    private Integer isSimilar;
    private Integer isTypical;
    private Integer isForcedClose;

    /** 是否挂账中：0否 1是 */
    private Integer isSuspended;
    /** 挂账恢复时间 */
    private LocalDateTime suspendUntil;
    /** 已批准延期次数 */
    private Integer extensionApprovedCount;

    // 回访信息
    private Integer needVisit;
    private String visitStatus;
    private String visitResult;
    private Integer visitSatisfaction;

    // 核查信息
    private Long verifyTaskId;
    private Long checkTaskId;

    // 申诉信息
    private String appealStatus;

    /** 处置超时申诉通过：统计按未超时，界面仍展示曾超时 */
    private Integer handleTimeoutExempt;

    private Long handleTimeoutExemptAppealId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    /** 是否待处置部门确认并批转派遣员（非表字段，详情/列表展示用） */
    @TableField(exist = false)
    private Boolean awaitingDeptConfirm;

    /** 是否待派遣员把关后批转受理员（非表字段） */
    @TableField(exist = false)
    private Boolean awaitingDispatcherForward;

    /** 是否有进行中的核查任务（可选分支，非表字段） */
    @TableField(exist = false)
    private Boolean pendingCheckTask;

    /** 是否有进行中的核查任务（可选分支，非表字段） */
    @TableField(exist = false)
    private Boolean pendingVerifyTask;

    /** 列表/详情：剩余时限文案（非表字段） */
    @TableField(exist = false)
    private String timeRemaining;

    /** 处置阶段剩余秒数（非表字段） */
    @TableField(exist = false)
    private Long handleRemainingSeconds;

    /** 处置是否已超时（非表字段） */
    @TableField(exist = false)
    private Boolean handleTimeout;

    /** 处置阶段计时是否曾超时（非表字段，含申诉通过后仍 true） */
    @TableField(exist = false)
    private Boolean handleStageTimedOut;

    /** 是否有待审批延期申请（非表字段） */
    @TableField(exist = false)
    private Boolean hasPendingExtension;

    /** 是否有待审批挂账申请（非表字段） */
    @TableField(exist = false)
    private Boolean hasPendingSuspend;

    /** 是否曾批准过挂账（非表字段，批准后不可再挂） */
    @TableField(exist = false)
    private Boolean suspendEverApproved;

    /** 待审批延期申请（非表字段，详情页派遣员审批用） */
    @TableField(exist = false)
    private CaseAdjustmentApply pendingExtensionApply;

    /** 待审批挂账申请（非表字段） */
    @TableField(exist = false)
    private CaseAdjustmentApply pendingSuspendApply;
}