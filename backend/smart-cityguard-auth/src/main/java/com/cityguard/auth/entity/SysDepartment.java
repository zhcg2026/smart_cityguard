package com.cityguard.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_department")
public class SysDepartment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deptName;

    private Long parentId;

    private Integer deptLevel;

    private Integer sortOrder;

    private Integer status;

    /** 部门登录账号（sys_user.id），用于部门级登录分派案件 */
    private Long loginUserId;

    /** 列表/详情展示：部门登录用户名 */
    @TableField(exist = false)
    private String loginUsername;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer isDeleted;
}