package com.cityguard.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityguard.auth.entity.SysDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SysDepartmentMapper extends BaseMapper<SysDepartment> {

    @Select("SELECT * FROM sys_department WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order")
    List<SysDepartment> selectByParentId(@Param("parentId") Long parentId);

    @Select("SELECT * FROM sys_department WHERE deleted = 0 ORDER BY sort_order")
    List<SysDepartment> selectAll();

    @Select("SELECT * FROM sys_department WHERE dept_level = #{deptLevel} AND deleted = 0 ORDER BY sort_order")
    List<SysDepartment> selectByDeptLevel(Integer deptLevel);

    @Update("UPDATE sys_department SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int logicDeleteById(@Param("id") Long id);

    /** 按部门名称查找已配置登录账号的部门 */
    @Select("SELECT * FROM sys_department WHERE dept_name = #{deptName} AND deleted = 0 " +
            "AND login_user_id IS NOT NULL LIMIT 1")
    SysDepartment selectActiveWithLoginByDeptName(@Param("deptName") String deptName);
}