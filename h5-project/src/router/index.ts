import {
  createRouter,
  createWebHashHistory,
  createWebHistory,
} from "vue-router";
import { currentH5Page } from "@/pages";

const routes = currentH5Page.routes?.length
  ? currentH5Page.routes
  : [
      {
        path: "/",
        name: currentH5Page.name,
        component: currentH5Page.loadComponent,
        meta: { title: currentH5Page.title },
      },
    ];

const router = createRouter({
  history:
    import.meta.env.VITE_SINGLE_FILE === "true"
      ? createWebHashHistory()
      : createWebHistory(import.meta.env.BASE_URL),
  routes,
});

// 全局路由守卫：更新页面标题
router.beforeEach((to) => {
  document.title = (to.meta.title as string | undefined) ?? currentH5Page.title;
});

export default router;
