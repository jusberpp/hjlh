<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { User, Lock } from "@element-plus/icons-vue";
import { useAuthStore } from "@/store/auth";
import { ApiError } from "@/utils/request";

const router = useRouter();
const authStore = useAuthStore();
const formRef = ref();
const loading = ref(false);

const form = reactive({
  username: "admin",
  password: "",
});

const rules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    await authStore.login({ username: form.username, password: form.password });
    ElMessage.success("登录成功");
    const redirect = router.currentRoute.value.query.redirect;
    router.replace(redirect || "/study/students");
  } catch (error) {
    const message = error instanceof ApiError ? error.message : "登录失败";
    ElMessage.error(message);
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="brand-mark">E</div>
        <div>
          <strong>E学慧通</strong>
          <span>学习认证管理系统</span>
        </div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" :prefix-icon="User" placeholder="用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :prefix-icon="Lock"
            show-password
            placeholder="密码"
          />
        </el-form-item>
        <el-button type="primary" class="login-button" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
      <p class="login-tip">演示账号 admin / Admin123!</p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e293b 0%, #2d3b50 60%, #1677ff 160%);
}

.login-card {
  width: 380px;
  padding: 36px 32px 28px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 20px 60px rgb(15 23 42 / 28%);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.brand-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 10px 3px 10px 3px;
  color: #fff;
  background: #1677ff;
  font-size: 22px;
  font-weight: 800;
}

.login-brand strong {
  display: block;
  color: #1f2a44;
  font-size: 18px;
}

.login-brand span {
  display: block;
  margin-top: 3px;
  color: #718096;
  font-size: 12px;
}

.login-button {
  width: 100%;
  margin-top: 6px;
}

.login-tip {
  margin: 16px 0 0;
  color: #98a2b3;
  font-size: 12px;
  text-align: center;
}
</style>
