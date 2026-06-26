import { ElMessage } from "element-plus";

const TOKEN_KEY = "e_study_admin_token";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY);
}

// 后端 LocalDateTime 使用 ISO 格式（带 T 分隔符）。
// 前端日期选择器使用 "YYYY-MM-DD HH:mm"，这里负责两者互转。
export function toBackendDateTime(value) {
  if (!value) return null;
  // "2026-07-01 09:00" -> "2026-07-01T09:00:00"
  const trimmed = String(value).trim().replace(" ", "T");
  return trimmed.length === 16 ? `${trimmed}:00` : trimmed;
}

export function toDisplayDateTime(value) {
  if (!value) return "";
  // "2026-07-01T09:00:00" 或 "2026-07-01T09:00:00.123" -> "2026-07-01 09:00"
  const text = String(value).replace("T", " ");
  return text.slice(0, 16);
}

// 统一请求方法。响应体约定：
//   普通:  { code, msg, data }
//   分页:  { code, msg, total, rows }
// code !== 200 视为业务失败。
async function request(url, options = {}) {
  const headers = { ...(options.headers || {}) };
  const token = getToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const isForm = options.body instanceof FormData;
  const isJson =
    options.body !== undefined &&
    options.body !== null &&
    !isForm &&
    !(options.body instanceof Blob);
  if (isJson && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
  if (isForm) delete headers["Content-Type"];
  if (isJson && typeof options.body !== "string") {
    options.body = JSON.stringify(options.body);
  }

  let response;
  try {
    response = await fetch(url, { ...options, headers });
  } catch (networkError) {
    ElMessage.error("网络异常，请检查后端服务是否启动");
    throw networkError;
  }

  const contentType = response.headers.get("content-type") || "";

  // 文件下载：后端返回二进制流（xlsx 等），失败时仍返回 JSON。
  if (options.responseType === "blob") {
    if (contentType.includes("application/json")) {
      const errorBody = await response.json().catch(() => ({}));
      throw new ApiError(errorBody.code || response.status, errorBody.msg || "下载失败");
    }
    if (!response.ok) throw new ApiError(response.status, "下载失败");
    // 透传文件名（后端通过 Content-Disposition 提供）。
    const disposition = response.headers.get("content-disposition") || "";
    const filenameMatch = /filename\*?=(?:UTF-8'')?([^;]+)/i.exec(disposition);
    const blob = await response.blob();
    blob.filename = filenameMatch
      ? decodeURIComponent(filenameMatch[1].replace(/["']/g, ""))
      : "";
    return blob;
  }

  const payload = await response.json().catch(() => ({}));
  if (response.status === 401) {
    removeToken();
    redirectToLogin();
    throw new ApiError(401, payload.msg || "未登录或登录已过期");
  }
  if (!response.ok && payload.code === undefined) {
    throw new ApiError(response.status, payload.msg || `请求失败 (${response.status})`);
  }
  if (payload.code !== undefined && payload.code !== 200) {
    throw new ApiError(payload.code, payload.msg || "操作失败");
  }
  return payload;
}

export class ApiError extends Error {
  constructor(code, message) {
    super(message);
    this.code = code;
  }
}

function redirectToLogin() {
  if (location.hash !== "#/login") {
    location.hash = "#/login";
  }
}

// 便捷方法：返回 data 字段（普通接口）。
export async function httpGet(url, params, options) {
  return request(buildUrl(url, params), { ...options, method: "GET" });
}
export async function httpPost(url, body, options) {
  return request(url, { ...options, method: "POST", body });
}
export async function httpPut(url, body, options) {
  return request(url, { ...options, method: "PUT", body });
}
export async function httpDelete(url, options) {
  return request(url, { ...options, method: "DELETE" });
}

// 便捷方法：直接取 data 字段。
export async function getData(url, params, options) {
  const payload = await httpGet(url, params, options);
  return payload.data;
}
export async function postData(url, body, options) {
  const payload = await httpPost(url, body, options);
  return payload.data;
}
export async function putData(url, body, options) {
  const payload = await httpPut(url, body, options);
  return payload.data;
}
export async function deleteData(url, options) {
  const payload = await httpDelete(url, options);
  return payload.data;
}

// 文件下载，返回 { blob, filename }。
export async function downloadBlob(url, params, options) {
  const blob = await request(buildUrl(url, params), {
    ...options,
    method: "GET",
    responseType: "blob",
  });
  return { blob, filename: blob.filename || "" };
}

// 文件上传（multipart），files 为 { 字段名: File | File[] }，extra 为附加表单字段。
export async function uploadFiles(url, files, extra = {}, options = {}) {
  const form = new FormData();
  Object.entries(extra).forEach(([key, value]) => {
    if (value !== undefined && value !== null) form.append(key, value);
  });
  Object.entries(files).forEach(([key, value]) => {
    const list = Array.isArray(value) ? value : [value];
    list.forEach((file) => form.append(key, file));
  });
  const payload = await request(buildUrl(url), {
    ...options,
    method: "POST",
    body: form,
  });
  return payload.data;
}

// 表单上传并直接返回 data（用于单字段多文件上传场景）。
export async function uploadAndGetData(url, files, extra, options) {
  return uploadFiles(url, files, extra, options);
}

function buildUrl(url, params) {
  if (!params) return url;
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.append(key, value);
    }
  });
  const query = search.toString();
  return query ? `${url}?${query}` : url;
}

// 触发浏览器保存文件。
export function saveBlob(blob, filename) {
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = filename || blob.filename || "download";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  setTimeout(() => URL.revokeObjectURL(link.href), 1000);
}
