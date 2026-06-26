import legacy from "@vitejs/plugin-legacy";
import vue from "@vitejs/plugin-vue";
import { VantResolver } from "@vant/auto-import-resolver";
import Components from "unplugin-vue-components/vite";
import { defineConfig, loadEnv, type Plugin } from "vite";
import { createReadStream, existsSync, readFileSync, statSync } from "node:fs";
import { extname, resolve } from "node:path";
import { fileURLToPath, URL } from "node:url";

const defaultH5Project = "summerTifenbao";
const virtualH5EntryId = "virtual:h5-entry";
const resolvedVirtualH5EntryId = `\0${virtualH5EntryId}`;

type H5Env = Record<string, string>;

const previewAssetMimeTypes: Record<string, string> = {
  ".css": "text/css; charset=utf-8",
  ".gif": "image/gif",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".webp": "image/webp",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
};

function createH5EntryPlugin(projectName: string): Plugin {
  return {
    name: "h5-entry",
    resolveId(id) {
      if (id === virtualH5EntryId) {
        return resolvedVirtualH5EntryId;
      }
    },
    load(id) {
      if (id !== resolvedVirtualH5EntryId) return;

      return `export { default as currentH5Page } from "/src/pages/${projectName}/entry.ts";`;
    },
  };
}

function createH5PreviewBasePlugin(basePath: string): Plugin {
  return {
    name: "h5-preview-base",
    configurePreviewServer(server) {
      if (basePath === "/") return;

      const baseAssetsPath = `${basePath}assets/`;
      const distAssetsPath = resolve(process.cwd(), "dist", "assets");

      server.middlewares.use((request, response, next) => {
        if (!request.url) {
          next();
          return;
        }

        if (request.url === "/") {
          response.statusCode = 302;
          response.setHeader("Location", basePath);
          response.end();
          return;
        }

        if (request.url.startsWith(baseAssetsPath)) {
          const assetRelativePath = decodeURIComponent(
            new URL(request.url, "http://localhost").pathname.slice(
              baseAssetsPath.length,
            ),
          );
          const assetPath = resolve(distAssetsPath, assetRelativePath);

          if (!assetPath.startsWith(`${distAssetsPath}/`)) {
            response.statusCode = 403;
            response.end();
            return;
          }

          if (existsSync(assetPath)) {
            const assetStat = statSync(assetPath);
            if (assetStat.isFile()) {
              response.statusCode = 200;
              response.setHeader(
                "Content-Type",
                previewAssetMimeTypes[extname(assetPath)] ??
                "application/octet-stream",
              );
              response.setHeader("Content-Length", assetStat.size);

              if (request.method === "HEAD") {
                response.end();
                return;
              }

              createReadStream(assetPath).pipe(response);
              return;
            }
          }
        }

        next();
      });
    },
  };
}

function toKebabCase(value: string) {
  return value.replace(/([a-z0-9])([A-Z])/g, "$1-$2").toLowerCase();
}

function normalizeBasePath(value: string) {
  const trimmedValue = value.trim();
  if (!trimmedValue || trimmedValue === "/") return "/";

  const withLeadingSlash = trimmedValue.startsWith("/")
    ? trimmedValue
    : `/${trimmedValue}`;

  return withLeadingSlash.endsWith("/")
    ? withLeadingSlash
    : `${withLeadingSlash}/`;
}

function resolvePreviewBasePathFromDist() {
  const distIndexPath = resolve(process.cwd(), "dist", "index.html");
  if (!existsSync(distIndexPath)) return;

  const distIndexHtml = readFileSync(distIndexPath, "utf8");
  const assetPathMatch = distIndexHtml.match(/\b(?:src|href)="(\/[^"]*?)assets\//);

  return assetPathMatch?.[1]
    ? normalizeBasePath(assetPathMatch[1])
    : undefined;
}

function resolveH5Project(env: H5Env) {
  const projectName = env.curProjectName || defaultH5Project;

  if (!/^[a-z][A-Za-z0-9]*$/.test(projectName)) {
    throw new Error(
      `Invalid curProjectName "${projectName}". Use a camelCase src/pages/<projectName> directory name.`,
    );
  }

  const entryPath = resolve(process.cwd(), "src/pages", projectName, "entry.ts");
  if (!existsSync(entryPath)) {
    throw new Error(
      `Cannot find H5 project entry: src/pages/${projectName}/entry.ts`,
    );
  }

  return projectName;
}

function resolveH5BasePath(
  projectName: string,
  env: H5Env,
  command: "build" | "serve",
  isPreview: boolean,
) {
  const configuredBasePath = env.H5_BASE_PATH || env.VITE_H5_BASE_PATH;
  if (configuredBasePath) {
    return normalizeBasePath(configuredBasePath);
  }

  if (command === "serve" && !isPreview) return "/";
  if (isPreview) {
    const distBasePath = resolvePreviewBasePathFromDist();
    if (distBasePath) return distBasePath;
  }

  return normalizeBasePath(toKebabCase(projectName));
}

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const h5Project = resolveH5Project(env);
  const isPreview = process.argv.includes("preview");
  const isSingleFileBuild = env.H5_SINGLE_FILE === "true";
  const h5BasePath = isSingleFileBuild
    ? "./"
    : resolveH5BasePath(h5Project, env, command, isPreview);

  return {
    base: h5BasePath,
    plugins: [
      createH5EntryPlugin(h5Project),
      createH5PreviewBasePlugin(h5BasePath),
      vue(),
      Components({
        resolvers: [VantResolver()],
        syncMode: "append",
      }),
      !isSingleFileBuild &&
        legacy({
          targets: ["Chrome >= 51", "iOS >= 10", "Android >= 5"],
          modernPolyfills: true,
        }),
    ].filter(Boolean),
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
      },
    },
    server: {
      host: "0.0.0.0",
    },
    build: {
      assetsDir: "assets",
      assetsInlineLimit: isSingleFileBuild ? 100_000_000 : undefined,
      cssCodeSplit: !isSingleFileBuild,
      modulePreload: isSingleFileBuild ? false : undefined,
      outDir: isSingleFileBuild ? "dist-single" : "dist",
      sourcemap: false,
      rollupOptions: isSingleFileBuild
        ? {
            output: {
              inlineDynamicImports: true,
            },
          }
        : undefined,
    },
  };
});
