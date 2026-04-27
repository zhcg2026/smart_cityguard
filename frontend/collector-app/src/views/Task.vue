<template>
  <div class="task">
    <van-nav-bar title="我的任务" />

    <van-tabs v-model:active="active">
      <van-tab title="未完成">
        <van-list finished finished-text="暂无任务">
          <van-cell-group inset>
            <van-cell title="核查任务：井盖缺失" value="待处理" is-link />
            <van-cell title="核实任务：路灯修复" value="待核实" is-link />
          </van-cell-group>
        </van-list>
      </van-tab>
      <van-tab title="已完成">
        <van-list finished finished-text="暂无记录">
          <van-cell-group inset>
            <van-cell title="核查任务：垃圾堆积" value="已完成" />
          </van-cell-group>
        </van-list>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
const active = ref(0)
</script>

<style scoped>
.task {
  min-height: 100vh;
  background: #f5f5f5;
}
</style>