package com.cityguard.timer.service;

import com.cityguard.common.exception.BusinessException;
import com.cityguard.timer.entity.HolidayConfig;
import com.cityguard.timer.entity.WorkTimeConfig;
import com.cityguard.timer.mapper.HolidayConfigMapper;
import com.cityguard.timer.mapper.WorkTimeConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimerConfigService {

    private final WorkTimeConfigMapper workTimeConfigMapper;
    private final HolidayConfigMapper holidayConfigMapper;

    public List<WorkTimeConfig> listWorkTimeConfigs() {
        return workTimeConfigMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkTimeConfig>()
                        .eq(WorkTimeConfig::getStatus, 1)
                        .orderByDesc(WorkTimeConfig::getIsDefault)
                        .orderByAsc(WorkTimeConfig::getId));
    }

    @Transactional
    public WorkTimeConfig updateWorkTimeConfig(Long id, WorkTimeConfig payload) {
        WorkTimeConfig entity = workTimeConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("工作时段配置不存在");
        }
        if (StringUtils.hasText(payload.getConfigName())) {
            entity.setConfigName(payload.getConfigName());
        }
        if (StringUtils.hasText(payload.getAmStartTime())) {
            entity.setAmStartTime(payload.getAmStartTime());
        }
        if (StringUtils.hasText(payload.getAmEndTime())) {
            entity.setAmEndTime(payload.getAmEndTime());
        }
        if (StringUtils.hasText(payload.getPmStartTime())) {
            entity.setPmStartTime(payload.getPmStartTime());
        }
        if (StringUtils.hasText(payload.getPmEndTime())) {
            entity.setPmEndTime(payload.getPmEndTime());
        }
        entity.setRemark(payload.getRemark());
        workTimeConfigMapper.updateById(entity);
        return entity;
    }

    public List<HolidayConfig> listHolidays(Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return holidayConfigMapper.selectByYear(y);
    }

    @Transactional
    public HolidayConfig saveHoliday(HolidayConfig payload) {
        if (payload.getHolidayDate() == null) {
            throw new BusinessException("请选择日期");
        }
        if (!StringUtils.hasText(payload.getHolidayType())) {
            throw new BusinessException("请选择类型");
        }
        if (!"holiday".equals(payload.getHolidayType()) && !"workday".equals(payload.getHolidayType())) {
            throw new BusinessException("类型须为 holiday 或 workday");
        }
        int year = payload.getHolidayDate().getYear();
        payload.setYear(year);
        if (payload.getSource() == null) {
            payload.setSource("manual");
        }

        if (payload.getId() != null) {
            holidayConfigMapper.updateById(payload);
            return payload;
        }

        HolidayConfig existing = holidayConfigMapper.selectByDate(payload.getHolidayDate());
        if (existing != null) {
            payload.setId(existing.getId());
            holidayConfigMapper.updateById(payload);
            return payload;
        }
        holidayConfigMapper.insert(payload);
        return payload;
    }

    @Transactional
    public void deleteHoliday(Long id) {
        if (id == null) {
            throw new BusinessException("ID 不能为空");
        }
        holidayConfigMapper.deleteById(id);
    }
}
