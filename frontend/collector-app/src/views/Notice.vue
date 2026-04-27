<template>
  <div class="notice">
    <van-nav-bar title="公文通告" left-arrow @click-left="$router.back()" />

    <van-list finished finished-text="暂无通告">
      <van-cell-group inset>
        <van-cell title="关于加强城市管理的通知" value="2024-04-27" is-link />
      </van-cell-group>
    </van-list>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.notice {
  min-height: 100vh;
  background: #f5f5f5;
}
</style>