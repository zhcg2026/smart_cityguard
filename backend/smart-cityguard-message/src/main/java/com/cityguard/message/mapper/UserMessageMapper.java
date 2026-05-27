package com.cityguard.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.message.entity.UserMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessage> {

    @Select("SELECT * FROM user_message WHERE user_id = #{userId} ORDER BY msg_time DESC, id DESC LIMIT #{limit}")
    List<UserMessage> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT * FROM user_message WHERE user_id = #{userId} AND msg_status = 'unread' "
            + "ORDER BY msg_time DESC, id DESC")
    List<UserMessage> selectUnreadByUserId(@Param("userId") Long userId);

    @Update("UPDATE user_message SET msg_status = 'read', read_time = NOW() WHERE id = #{id} AND user_id = #{userId}")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE user_message SET msg_status = 'read', read_time = NOW() "
            + "WHERE user_id = #{userId} AND msg_status = 'unread'")
    int markAllAsRead(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_message WHERE user_id = #{userId} AND msg_status = 'unread'")
    int countUnread(@Param("userId") Long userId);
}
