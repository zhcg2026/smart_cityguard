package com.cityguard.common.spi;

/**
 * 用户生命周期清理钩子（删除用户时解绑跨模块关联数据）。
 */
public interface UserLifecycleCleanup {

    void onUserDeleted(Long userId);
}
