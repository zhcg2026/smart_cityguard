package com.cityguard.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.auth.entity.SysDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDepartmentMapper extends BaseMapper<SysDepartment> {

    @Select("SELECT * FROM sys_department WHERE parent_id = #{parentId} AND is_deleted = 0 ORDER BY sort")
    List<SysDepartment> selectByParentId(Long parentId);

    @Select("SELECT * FROM sys_department WHERE dept_type = #{deptType} AND is_deleted = 0 ORDER BY sort")
    List<SysDepartment> selectByDeptType(Integer deptType);
}