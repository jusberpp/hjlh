<template>
  <div class="auth-step">
    <PageHeader title="绑定看课手机号" show-back @back="emit('back')" />

    <div class="auth-content auth-content--bind">
      <!-- 严重提示 -->
      <div class="auth-warning">
        <p class="auth-warning-title">
          <van-icon name="warning" class="auth-warning-icon" />
          严重提示：
        </p>
        <p class="auth-warning-text">
          请仔细检查您填写的手机号！该手机号将作为小鹅通登录凭证，
          <strong>填写提交后不支持自助修改</strong>
          。如需修改，请扫码联系下方客服。
        </p>
      </div>

      <!-- 手机号表单 -->
      <p class="auth-field-label auth-field-label--strong">看课手机号</p>
      <div
        class="auth-control auth-control--input"
        :class="{ 'auth-control--error': phoneError }"
      >
        <input
          v-model="phone"
          class="auth-input"
          type="tel"
          placeholder="请输入11位手机号"
          maxlength="11"
          inputmode="numeric"
          @input="phone = phone.replace(/\D/g, '')"
          @blur="validatePhone"
        />
      </div>
      <p v-if="phoneError" class="auth-error">{{ phoneError }}</p>

      <div class="auth-btn-wrap auth-btn-wrap--spaced">
        <van-button
          class="auth-primary-btn"
          block
          :loading="loading"
          @click="handleSubmit"
        >
          确认提交并开通
        </van-button>
      </div>

      <!-- 客服二维码 -->
      <div v-if="qrImage" class="auth-qr-section">
        <p class="auth-qr-hint">遇到问题？请联系客服</p>
        <div class="auth-qr-border">
          <img :src="qrImage" alt="客服二维码" class="auth-qr-img" />
        </div>
      </div>
    </div>

    <!-- 核对手机号弹窗 -->
    <van-dialog v-model:show="showConfirmDialog" :show-confirm-button="false">
      <div class="auth-dialog-content">
        <p class="auth-dialog-title">请核对看课手机号</p>
        <p class="auth-dialog-desc">您填写的手机号为：</p>
        <p class="auth-dialog-phone">{{ phone }}</p>
        <p class="auth-dialog-warning">
          <van-icon name="warning" color="#e53935" />
          提交后不支持自助修改！
        </p>
        <div class="auth-dialog-actions">
          <van-button
            class="auth-dialog-btn auth-dialog-btn--cancel"
            @click="showConfirmDialog = false"
          >
            返回修改
          </van-button>
          <van-button
            class="auth-dialog-btn auth-dialog-btn--confirm"
            type="primary"
            :loading="loading"
            @click="doBindPhone"
          >
            确认无误
          </van-button>
        </div>
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { bindPhone } from "../api";
import type { VerifyStudentRes } from "../api";
import PageHeader from "./PageHeader.vue";

const props = defineProps<{
  studentInfo: VerifyStudentRes;
  qrImage?: string;
}>();

const emit = defineEmits<{
  back: [];
  success: [];
}>();

const phone = ref("");
const phoneError = ref("");
const loading = ref(false);
const showConfirmDialog = ref(false);

function validatePhone() {
  if (!phone.value) {
    phoneError.value = "请输入手机号";
    return false;
  }
  if (!/^1\d{10}$/.test(phone.value)) {
    phoneError.value = "请输入正确的11位手机号";
    return false;
  }
  phoneError.value = "";
  return true;
}

function handleSubmit() {
  if (loading.value) return;
  if (!validatePhone()) return;
  showConfirmDialog.value = true;
}

async function doBindPhone() {
  loading.value = true;
  try {
    await bindPhone(
      { id: props.studentInfo.id, phone: phone.value },
      props.studentInfo.token,
    );
    showConfirmDialog.value = false;
    emit("success");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped lang="scss">
:deep(.van-dialog) {
  width: 600px;
  border-radius: 30px;
}
</style>
