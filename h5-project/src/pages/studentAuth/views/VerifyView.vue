<template>
  <StepVerify
    :initial-form="authStore.savedVerifyForm"
    @success="onSuccess"
    @form-change="authStore.setSavedVerifyForm"
  />
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import StepVerify from "../components/StepVerify.vue";
import type { VerifyStudentRes } from "../api";
import { fetchBindResult } from "../api";
import { useAuthStore } from "../store";

const router = useRouter();
const authStore = useAuthStore();

async function onSuccess(data: VerifyStudentRes) {
  authStore.setStudentInfo(data);

  if (data.phone) {
    // 已绑定：直接拉 bind-result 展示成功页
    const result = await fetchBindResult(data.token);
    authStore.setBindResult(result);
    router.replace({ name: "studentAuthSuccess" });
  } else {
    router.push({ name: "studentAuthConfirm" });
  }
}
</script>
