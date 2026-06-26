/// <reference types="vite/client" />

declare module "virtual:h5-entry" {
  import type { H5PageEntry } from "./pages/types";

  export const currentH5Page: H5PageEntry;
}
