import { createRouter, createWebHashHistory } from "vue-router";
import AdminLayout from "@/layout/AdminLayout.vue";
import { useAuthStore } from "@/store/auth";

const routes = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/login/index.vue"),
    meta: { title: "登录", public: true },
  },
  {
    path: "/",
    component: AdminLayout,
    redirect: "/study/students",
    children: [
      {
        path: "study/students",
        name: "StudentManagement",
        component: () => import("@/views/student/index.vue"),
        meta: { title: "学员信息管理", group: "学习信息管理" },
      },
      {
        path: "course/list",
        name: "CourseManagement",
        component: () => import("@/views/course/index.vue"),
        meta: { title: "课程管理", group: "学科课程管理" },
      },
      {
        path: "course/materials",
        name: "MaterialManagement",
        component: () => import("@/views/material/index.vue"),
        meta: { title: "课程资料管理", group: "学科课程管理" },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  if (to.meta.public) {
    if (authStore.isLoggedIn && to.name === "Login") {
      return { path: "/study/students" };
    }
    return true;
  }
  if (!authStore.isLoggedIn) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  return true;
});

export default router;
