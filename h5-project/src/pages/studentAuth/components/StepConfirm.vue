<template>
  <div class="auth-step">
    <PageHeader title="确认报名信息" show-back @back="emit('back')" />

    <div class="auth-content">
      <!-- 学生信息卡片 -->
      <div class="auth-info-panel auth-info-panel--student border-rd-12px">
        <div class="auth-info-row">
          <span class="auth-info-key">学校：</span>
          <span class="auth-info-val">{{ info.school }}</span>
        </div>
        <div class="auth-info-row">
          <span class="auth-info-key">年级：</span>
          <span class="auth-info-val">{{ info.grade }}</span>
        </div>
        <div class="auth-info-row">
          <span class="auth-info-key">姓名：</span>
          <span class="auth-info-val">{{ info.studentName }}</span>
        </div>
        <div class="auth-info-row">
          <span class="auth-info-key">学号：</span>
          <span class="auth-info-val">{{ info.studentId }}</span>
        </div>
      </div>

      <!-- 课程信息卡片 -->
      <div class="auth-info-panel auth-info-panel--course">
        <p class="auth-course-label">已报名课程：</p>
        <p class="auth-course-name">{{ info.goodsName }}</p>
        <p v-if="info.learningLevels" class="auth-course-level">
          （分层信息：{{ info.learningLevels }}）
        </p>
      </div>

      <van-button
        class="auth-primary-btn confirm-submit"
        type="primary"
        block
        @click="emit('confirm')"
      >
        立即开通看课权限
      </van-button>

      <p class="auth-link-muted" @click="emit('back')">返回修改信息</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { VerifyStudentRes } from "../api";
import PageHeader from "./PageHeader.vue";

defineProps<{
  info: VerifyStudentRes;
}>();

const emit = defineEmits<{
  confirm: [];
  back: [];
}>();
</script>

<style scoped lang="scss">
.confirm-submit {
  margin-top: 60px;
}
</style>
