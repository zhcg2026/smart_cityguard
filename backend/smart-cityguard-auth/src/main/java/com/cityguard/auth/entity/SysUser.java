package com.cityguard.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String email;

    private String avatar;

    private Long departmentId;

    /** 列表/详情展示用，非表字段 */
    @TableField(exist = false)
    private String departmentName;

    /** 列表展示：角色中文名，逗号分隔 */
    @TableField(exist = false)
    private String roleNames;

    /** 系统内置管理员：无部门、不可被编辑/删除 */
    @TableField(exist = false)
    private Boolean systemProtected;

    /** 部门登录账号：仅可通过部门管理维护 */
    @TableField(exist = false)
    private Boolean deptLoginAccount;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}