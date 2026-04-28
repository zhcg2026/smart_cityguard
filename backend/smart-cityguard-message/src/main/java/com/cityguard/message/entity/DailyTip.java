package com.cityguard.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("daily_tip")
public class DailyTip {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;

    private Integer sort;

    private LocalDateTime publishDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Integer status;
}