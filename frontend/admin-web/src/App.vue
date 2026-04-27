<template>
  <el-container class="layout">
    <el-aside width="200px">
      <el-menu :default-active="$route.path" router>
        <div class="logo">智慧城管</div>
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-sub-menu index="/case">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>案件管理</span>
          </template>
          <el-menu-item index="/case/register">登记栏</el-menu-item>
          <el-menu-item index="/case/pending">待查栏</el-menu-item>
          <el-menu-item index="/case/filing">立案栏</el-menu-item>
          <el-menu-item index="/case/check">核查栏</el-menu-item>
          <el-menu-item index="/case/handled">经办案件</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/standard">
          <el-icon><List /></el-icon>
          <span>立结案标准</span>
        </el-menu-item>
        <el-menu-item index="/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">管理员</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
</script>

<style>
.layout {
  height: 100vh;
}
.el-aside {
  background: #304156;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}
.el-menu {
  border-right: none;
  background: #304156;
}
.el-header {
  background: #fff;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}
.header-right {
  display: flex;
  align-items: center;
}
.user-info {
  cursor: pointer;
}
</style>