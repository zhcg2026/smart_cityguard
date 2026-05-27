package com.cityguard.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.message.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {

    @Select("SELECT * FROM announcement WHERE status = 'published' AND is_deleted = 0 AND (expire_time IS NULL OR expire_time > NOW()) ORDER BY is_top DESC, top_order DESC, publish_time DESC")
    List<Announcement> selectPublished();

    @Select("SELECT * FROM announcement WHERE is_deleted = 0 ORDER BY publish_time DESC, id DESC")
    List<Announcement> selectAllAdmin();
}