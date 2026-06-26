<script setup>
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessageBox } from "element-plus";
import {
  ArrowDown,
  Bell,
  Collection,
  Expand,
  Fold,
  FullScreen,
  Reading,
  Setting,
  SwitchButton,
  UserFilled,
} from "@element-plus/icons-vue";
import { useAuthStore } from "@/store/auth";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const collapsed = ref(false);

const activeMenu = computed(() => route.path);
const pageTitle = computed(() => route.meta.title || "管理控制台");
const pageGroup = computed(() => route.meta.group || "首页");
const displayName = computed(() => authStore.displayName);

const navigate = (path) => {
  if (path) router.push(path);
};

const handleCommand = (command) => {
  if (command === "logout") {
    ElMessageBox.confirm("确认退出登录吗？", "退出登录", {
      type: "warning",
      confirmButtonText: "退出",
    }).then(() => {
      authStore.logout();
      router.replace("/login");
    }).catch(() => {});
  }
};
</script>

<template>
  <div class="admin-shell" :class="{ collapsed }">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">E</div>
        <div v-show="!collapsed" class="brand-copy">
          <strong>E学慧通</strong>
          <span>学习认证管理系统</span>
        </div>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          class="sidebar-menu"
          unique-opened
          @select="navigate"
        >
          <el-sub-menu index="study">
            <template #title>
              <el-icon><UserFilled /></el-icon>
              <span>学习信息管理</span>
            </template>
            <el-menu-item index="/study/students">学员信息管理</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="course">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span>学科课程管理</span>
            </template>
            <el-menu-item index="/course/list">课程管理</el-menu-item>
            <el-menu-item index="/course/materials">课程资料管理</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>

      <div class="sidebar-foot" :title="collapsed ? '系统设置' : ''">
        <el-icon><Setting /></el-icon>
        <span v-show="!collapsed">系统设置</span>
      </div>
    </aside>

    <section class="main-panel">
      <header class="topbar">
        <div class="topbar-left">
          <button class="icon-button" type="button" @click="collapsed = !collapsed">
            <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </button>
          <div class="breadcrumb">
            <span>{{ pageGroup }}</span>
            <i>/</i>
            <strong>{{ pageTitle }}</strong>
          </div>
        </div>
        <div class="topbar-actions">
          <span class="environment-tag">演示环境</span>
          <button class="icon-button" type="button" title="全屏">
            <el-icon><FullScreen /></el-icon>
          </button>
          <button class="icon-button notification" type="button" title="通知">
            <el-icon><Bell /></el-icon>
            <i></i>
          </button>
          <div class="operator">
            <span class="avatar">{{ displayName.slice(0, 1) }}</span>
            <div class="operator-copy">
              <strong>{{ displayName }}</strong>
              <span>运营中心</span>
            </div>
            <el-dropdown trigger="click" @command="handleCommand">
              <el-icon class="operator-arrow"><ArrowDown /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </header>

      <div class="page-tabs">
        <div class="page-tab active">
          <el-icon><Reading /></el-icon>
          <span>{{ pageTitle }}</span>
        </div>
      </div>

      <main class="app-main">
        <router-view />
      </main>
    </section>
  </div>
</template>
