import type { RouteRecordRaw } from "vue-router";

const routes: RouteRecordRaw[] = [
  {
    path: "/",
    component: () => import("./index.vue"),
    children: [
      {
        path: "",
        redirect: { name: "studentAuthVerify" },
      },
      {
        path: "verify",
        name: "studentAuthVerify",
        component: () => import("./views/VerifyView.vue"),
        meta: { title: "学员身份认证" },
      },
      {
        path: "confirm",
        name: "studentAuthConfirm",
        component: () => import("./views/ConfirmView.vue"),
        meta: { title: "确认报名信息" },
      },
      {
        path: "bind-phone",
        name: "studentAuthBindPhone",
        component: () => import("./views/BindPhoneView.vue"),
        meta: { title: "绑定看课手机号" },
      },
      {
        path: "success",
        name: "studentAuthSuccess",
        component: () => import("./views/SuccessView.vue"),
        meta: { title: "课程已开通" },
      },
    ],
  },
];

export default routes;
