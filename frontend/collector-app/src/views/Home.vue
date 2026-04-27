<template>
  <div class="home">
    <van-nav-bar title="智慧城管" />

    <van-cell-group inset title="今日提示">
      <van-cell title="暂无提示信息" />
    </van-cell-group>

    <van-grid :column-num="3" class="menu-grid">
      <van-grid-item icon="edit" text="问题上报" to="/report" />
      <van-grid-item icon="todo-list" text="我的任务" to="/task" />
      <van-grid-item icon="notification" text="公文通告" to="/notice" />
    </van-grid>

    <van-tabbar v-model="active">
      <van-tabbar-item icon="home-o" to="/home">首页</van-tabbar-item>
      <van-tabbar-item icon="edit" to="/report">上报</van-tabbar-item>
      <van-tabbar-item icon="todo-list" to="/task">任务</van-tabbar-item>
      <van-tabbar-item icon="user-o" to="/profile">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
const active = ref(0)
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: #f5f5f5;
}
.menu-grid {
  margin: 16px 0;
}
</style>