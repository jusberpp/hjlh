<template>
  <StepBindPhone
    v-if="authStore.studentInfo"
    :student-info="authStore.studentInfo"
    :qr-image="pageConfig.qrImage"
    @back="router.back()"
    @success="onSuccess"
  />
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import StepBindPhone from "../components/StepBindPhone.vue";
import { fetchBindResult } from "../api";
import { pageConfig } from "../data";
import { useAuthStore } from "../store";

const router = useRouter();
const authStore = useAuthStore();

async function onSuccess() {
  const result = await fetchBindResult(authStore.token);
  authStore.setBindResult(result);
  router.replace({ name: "studentAuthSuccess" });
}
</script>
