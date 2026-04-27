<template>
  <div class="report">
    <van-nav-bar title="问题上报" left-arrow @click-left="$router.back()" />

    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field label="问题类型" is-link readonly v-model="categoryName" placeholder="请选择" @click="showCategory = true" />
        <van-field label="立案条件" is-link readonly v-model="conditionName" placeholder="请选择" @click="showCondition = true" />
        <van-field label="定位地址" v-model="location" placeholder="点击地图选择位置">
          <template #button>
            <van-button size="small" type="primary">地图</van-button>
          </template>
        </van-field>
        <van-field v-model="description" label="问题描述" type="textarea" rows="3" placeholder="请描述问题情况" />
      </van-cell-group>

      <van-cell-group inset title="附件上传">
        <van-uploader v-model="fileList" multiple :max-count="5" />
      </van-cell-group>

      <div class="submit-btn">
        <van-button block type="primary" native-type="submit">提交上报</van-button>
      </div>
    </van-form>

    <!-- 问题类型选择器 -->
    <van-popup v-model:show="showCategory" position="bottom" round>
      <van-picker title="选择问题类型" :columns="categoryColumns" @confirm="onCategoryConfirm" @cancel="showCategory = false" />
    </van-popup>

    <!-- 立案条件选择器 -->
    <van-popup v-model:show="showCondition" position="bottom" round>
      <van-picker title="选择立案条件" :columns="conditionColumns" @confirm="onConditionConfirm" @cancel="showCondition = false" />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const categoryName = ref('')
const conditionName = ref('')
const location = ref('')
const description = ref('')
const fileList = ref([])
const showCategory = ref(false)
const showCondition = ref(false)

const categoryColumns = [
  { text: '部件类', children: [
    { text: '公用设施', children: ['上水井盖', '污水井盖', '雨水井盖', '路灯', '电力设施'] },
    { text: '道路交通设施', children: ['停车场', '公交站亭', '交通护栏', '路名牌'] },
    { text: '市容环境设施', children: ['公共厕所', '垃圾箱', '户外广告'] },
  ]},
  { text: '事件类', children: [
    { text: '市容环境', children: ['暴露垃圾', '道路不洁', '乱堆物堆料'] },
  ]}
]

const conditionColumns = ['破损', '缺失', '移位', '倾斜']

const onCategoryConfirm = (val: any) => {
  categoryName.value = val.selectedOptions.map(o => o.text).join('-')
  showCategory.value = false
}

const onConditionConfirm = (val: any) => {
  conditionName.value = val.selectedOptions[val.selectedOptions.length - 1].text
  showCondition.value = false
}

const onSubmit = () => {
  // TODO: 提交上报
}
</script>

<style scoped>
.report {
  min-height: 100vh;
  background: #f5f5f5;
}
.submit-btn {
  padding: 16px;
}
</style>