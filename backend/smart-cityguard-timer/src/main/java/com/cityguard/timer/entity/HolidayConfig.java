package com.cityguard.timer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("holiday_config")
public class HolidayConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer year;

    private LocalDate holidayDate;

    /** holiday=法定节假日，workday=调休工作日 */
    private String holidayType;

    private String holidayName;

    private String remark;

    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
