<template>
  <div class="page-header">
    <img class="header-bg" :src="headBg" alt="" aria-hidden="true" />
    <div class="header-inner">
      <button
        v-if="showBack && isBackButtonEnabled"
        class="back-btn"
        type="button"
        aria-label="返回"
        @click="emit('back')"
      >
        <img :src="backArrow" alt="" aria-hidden="true" class="back-icon" />
      </button>
      <!-- 无返回按钮时用占位块保证标题水平居中 -->
      <span v-else class="back-placeholder" aria-hidden="true" />
      <h1 class="header-title">{{ title }}</h1>
      <span class="end-placeholder" aria-hidden="true" />
    </div>
  </div>
</template>

<script setup lang="ts">
import backArrow from "../assets/back_arrow.png";
import headBg from "../assets/head_bg.png";

defineProps<{
  title: string;
  showBack?: boolean;
}>();

// 当前版本按产品要求暂不展示顶部返回按钮；保留 showBack/back 接口，后续需要时只需开启此开关。
const isBackButtonEnabled = false;

const emit = defineEmits<{
  back: [];
}>();
</script>
