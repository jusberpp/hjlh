import type { H5PageEntry } from "../types";
import routes from "./routes";

const entry: H5PageEntry = {
  name: "studentAuth",
  title: "学习认证",
  loadComponent: () => import("./index.vue"),
  routes,
};

export default entry;
