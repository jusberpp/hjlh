<template>
  <div class="auth-step step-success">
    <!-- 顶部区域：head_bg + 返回 + success 图（fixed） -->
    <div class="hero">
      <img class="hero-bg" :src="headBg" alt="" aria-hidden="true" />
      <div class="hero-inner">
        <button
          v-if="isBackButtonEnabled"
          class="back-btn"
          type="button"
          aria-label="返回"
          @click="emit('back')"
        >
          <img :src="backArrow" alt="" class="back-icon" aria-hidden="true" />
        </button>
      </div>
    </div>

    <div class="success-content">
      <!-- success 图标 -->
      <img class="success-img" :src="successImg" alt="课程已成功开通" />

      <h1 class="success-title">课程已成功开通！</h1>
      <p class="success-desc">请使用下方手机号登录小鹅通进行学习</p>

      <!-- 听课账号 -->
      <div class="auth-account-card">
        <span>听课账号：</span>
        <span class="font-bold">{{ result.phone }}</span>
      </div>

      <!-- 课程卡片 -->
      <div class="auth-result-card">
        <div v-if="result.goodsImg">
          <img
            :src="result.goodsImg"
            alt="课程封面"
            class="auth-result-card__image"
          />
        </div>
        <div class="auth-result-card__body">
          <p class="auth-result-card__title">{{ result.goodsName }}</p>
        </div>
      </div>

      <!-- 配套资料 -->
      <div v-if="result.files.length > 0" class="auth-files-card">
        <p class="auth-files-title">
          <span class="auth-files-title-bar"></span>
          配套学习资料
        </p>
        <div class="auth-file-list">
          <div
            v-for="file in result.files"
            :key="file.id"
            class="auth-file-item"
          >
            <div class="auth-file-icon" :class="fileIconClass(file.fileName)">
              {{ fileExt(file.fileName) }}
            </div>
            <div class="auth-file-meta">
              <p class="auth-file-name">{{ file.fileName }}</p>
              <p class="auth-file-info">
                {{ formatSize(file.fileSize) }} ｜
                {{ formatDate(file.updateTime) }}
              </p>
            </div>
            <button
              class="auth-download-btn"
              type="button"
              :disabled="downloading[file.id]"
              @click="handleDownload(file)"
            >
              {{ downloading[file.id] ? "下载中…" : "下载" }}
            </button>
          </div>
        </div>
      </div>

      <!-- 去上课 -->
      <a
        class="auth-action-link"
        :href="result.courseUrl"
        target="_blank"
        rel="noopener noreferrer"
      >
        立即去小鹅通上课
      </a>

      <!-- 客服二维码 -->
      <div v-if="qrImage" class="auth-qr-section">
        <p class="auth-qr-desc">
          如遇无法登录、课程未到账等问题<br />请扫码联系专属辅导老师
        </p>
        <div class="auth-qr-border">
          <img :src="qrImage" alt="客服二维码" class="auth-qr-img" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from "vue";
import backArrow from "../assets/back_arrow.png";
import headBg from "../assets/head_bg.png";
import successImg from "../assets/success.png";
import { downloadFile } from "../api";
import type { BindResultRes, CourseFile } from "../api";

const props = defineProps<{
  result: BindResultRes;
  token: string;
  qrImage?: string;
}>();

const emit = defineEmits<{
  back: [];
}>();

// 当前版本暂不开放成功页返回入口，避免用户误返回后清空开通结果；保留代码便于后续恢复。
const isBackButtonEnabled = false;

/** 记录正在下载中的文件 id，防止重复点击 */
const downloading = reactive<Record<string, boolean>>({});

async function handleDownload(file: CourseFile) {
  if (downloading[file.id]) return;
  downloading[file.id] = true;
  try {
    const { blob, filename } = await downloadFile(file.id, props.token);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename || file.fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  } finally {
    downloading[file.id] = false;
  }
}

function fileExt(name: string) {
  return name.split(".").pop()?.toUpperCase() ?? "FILE";
}

function fileIconClass(name: string) {
  const ext = name.split(".").pop()?.toLowerCase() ?? "";
  if (ext === "pdf") return "icon-pdf";
  if (["doc", "docx"].includes(ext)) return "icon-doc";
  return "icon-default";
}

function formatSize(bytes: number) {
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${bytes} B`;
}

function formatDate(dateStr: string) {
  return dateStr.slice(0, 10);
}
</script>

<style scoped lang="scss">
.step-success {
  padding-top: 196px;
}

/* 顶部 hero - fixed 定位 */
.hero {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
}

.hero-bg {
  display: block;
  width: 100%;
  height: 320px;
  object-fit: cover;
}

.hero-inner {
  position: absolute;
  inset: 0;
  padding: 0;
}

.back-btn {
  position: absolute;
  top: 91px;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 82px;
  height: 82px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.back-icon {
  width: 34px;
  height: 34px;
  object-fit: contain;
}

/* 主内容 */
.success-content {
  padding: 0 30px;
  position: relative;
}

.success-img {
  display: block;
  margin: 0 auto 30px;
  width: 300px;
  height: auto;
  object-fit: contain;
}

.success-title {
  margin: 0 0 70px;
  color: #054ffc;
  font-size: 34px;
  font-weight: 700;
  line-height: 34px;
  text-align: center;
}

.success-desc {
  margin: 0 0 30px;
  color: #979797;
  font-size: 28px;
  line-height: 28px;
  text-align: center;
}

/* 听课账号 */
</style>
