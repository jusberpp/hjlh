import type { H5PageEntry } from "../types";

const entry: H5PageEntry = {
  name: "summerTifenbao",
  title: "暑期课程提分宝",
  loadComponent: () => import("./index.vue"),
};

export default entry;
