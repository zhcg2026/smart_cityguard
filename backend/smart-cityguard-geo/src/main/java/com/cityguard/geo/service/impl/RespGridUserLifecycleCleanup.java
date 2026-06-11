package com.cityguard.geo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cityguard.common.spi.UserLifecycleCleanup;
import com.cityguard.geo.entity.RespGridCollector;
import com.cityguard.geo.mapper.RespGridCollectorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RespGridUserLifecycleCleanup implements UserLifecycleCleanup {

    private final RespGridCollectorMapper collectorMapper;

    @Override
    public void onUserDeleted(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }
        collectorMapper.delete(new LambdaQueryWrapper<RespGridCollector>()
                .eq(RespGridCollector::getUserId, userId));
    }
}
