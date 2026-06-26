import { execFileSync } from "node:child_process";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const projectDir = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const buildDir = resolve(projectDir, "dist-single");
const outputFile = resolve(
  projectDir,
  "output/html/E学慧通暑期课程提分宝系统.html",
);

execFileSync(
  resolve(projectDir, "node_modules/.bin/vite"),
  ["build", "--mode", "singlefile"],
  {
    cwd: projectDir,
    stdio: "inherit",
  },
);

let html = readFileSync(resolve(buildDir, "index.html"), "utf8");

html = html.replace(
  /<link rel="stylesheet" crossorigin href="\.\/([^"]+)">/g,
  (_, assetPath) => {
    const css = readFileSync(resolve(buildDir, assetPath), "utf8");
    return `<style>${css}</style>`;
  },
);

html = html.replace(
  /<script type="module" crossorigin src="\.\/([^"]+)"><\/script>/g,
  (_, assetPath) => {
    const script = readFileSync(resolve(buildDir, assetPath), "utf8");
    return `<script type="module">${script}</script>`;
  },
);

html = html
  .replace(
    "<title>高考备考指导暨考后填报公开课</title>",
    "<title>E学慧通暑期课程提分宝系统</title>",
  )
  .replace(
    "</head>",
    `<meta name="description" content="E学慧通学生端 H5 与运营后台交互原型，单文件离线版" />
  </head>`,
  );

if (/\.\/assets\//.test(html)) {
  throw new Error("Single HTML still contains external asset references.");
}

mkdirSync(dirname(outputFile), { recursive: true });
writeFileSync(outputFile, html);

console.log(`\nSingle HTML created:\n${outputFile}`);
