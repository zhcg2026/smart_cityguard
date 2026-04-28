package com.cityguard.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityguard.message.entity.UserMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessage> {

    @Select("SELECT * FROM user_message WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<UserMessage> selectByUserId(Long userId);

    @Select("SELECT * FROM user_message WHERE user_id = #{userId} AND is_read = 0 AND is_deleted = 0 ORDER BY create_time DESC")
    List<UserMessage> selectUnreadByUserId(Long userId);

    @Update("UPDATE user_message SET is_read = 1 WHERE id = #{id}")
    int markAsRead(Long id);

    @Update("UPDATE user_message SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllAsRead(Long userId);

    @Select("SELECT COUNT(*) FROM user_message WHERE user_id = #{userId} AND is_read = 0 AND is_deleted = 0")
    int countUnread(Long userId);
}