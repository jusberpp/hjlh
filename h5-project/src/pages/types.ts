import type { Component } from "vue";
import type { RouteRecordRaw } from "vue-router";

export interface H5PageEntry {
  name: string;
  title: string;
  loadComponent: () => Promise<{ default: Component }>;
  routes?: RouteRecordRaw[];
}
