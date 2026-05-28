package com.cityguard.appeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.appeal.entity.AppealReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AppealReviewMapper extends BaseMapper<AppealReview> {

    @Select("SELECT * FROM appeal_review WHERE appeal_id = #{appealId} ORDER BY review_time ASC")
    List<AppealReview> selectByAppealId(Long appealId);
}
