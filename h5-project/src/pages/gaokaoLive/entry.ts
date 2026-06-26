import type { H5PageEntry } from "../types";
import routes from "./routes";

const entry: H5PageEntry = {
  name: "gaokaoLive",
  title: "高考直播",
  loadComponent: () => import("./index.vue"),
  routes,
};

export default entry;
