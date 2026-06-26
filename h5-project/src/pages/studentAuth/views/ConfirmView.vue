<template>
  <StepConfirm
    v-if="authStore.studentInfo"
    :info="authStore.studentInfo"
    @confirm="router.push({ name: 'studentAuthBindPhone' })"
    @back="router.back()"
  />
</template>

<script setup lang="ts">
import { useRouter } from "vue-router";
import StepConfirm from "../components/StepConfirm.vue";
import { useAuthStore } from "../store";

const router = useRouter();
const authStore = useAuthStore();
</script>
