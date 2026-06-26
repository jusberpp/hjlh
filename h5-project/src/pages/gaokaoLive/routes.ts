import type { RouteRecordRaw } from "vue-router";

const routes: RouteRecordRaw[] = [
  {
    path: "/",
    name: "gaokaoLive",
    component: () => import("./index.vue"),
    meta: { title: "高考直播" },
  },
];

export default routes;
