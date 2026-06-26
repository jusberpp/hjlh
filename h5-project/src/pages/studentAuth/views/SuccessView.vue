<template>
  <StepSuccess
    v-if="authStore.bindResult && authStore.token"
    :result="authStore.bindResult"
    :token="authStore.token"
    :qr-image="pageConfig.qrImage"
    @back="onBack"
  />
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import StepSuccess from "../components/StepSuccess.vue";
import { pageConfig } from "../data";
import { useAuthStore } from "../store";

const router = useRouter();
const authStore = useAuthStore();

function onBack() {
  // 成功页返回：清空状态，重新验证
  authStore.resetAll();
  router.replace({ name: "studentAuthVerify" });
}
</script>
