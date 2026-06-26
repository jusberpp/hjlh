/**
 * HTTP 请求封装
 *
 * 基于 axios，统一处理：
 * - 请求拦截：注入 token（存在时）
 * - 响应拦截：解包业务数据、识别业务错误码、网络异常 Toast 提示
 *
 * 使用示例：
 *   import { get, post } from '@/utils/request'
 *
 *   // 普通 GET
 *   const data = await get<CourseList>('/api/courses')
 *
 *   // 带参数 POST
 *   const result = await post<SignResult>('/api/sign', { url: location.href })
 */

import axios, {
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import { showToast } from "vant";
import "vant/es/toast/style";

// ─── 类型 ────────────────────────────────────────────────────────────────────

/** 后端统一响应结构 */
export interface ApiResponse<T = unknown> {
  code: number;
  msg: string;
  data: T;
}

/** 请求层抛出的业务错误 */
export class ApiError extends Error {
  constructor(
    public readonly code: number,
    msg: string,
  ) {
    super(msg);
    this.name = "ApiError";
  }
}

// ─── 实例配置 ────────────────────────────────────────────────────────────────

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "",
  timeout: 15_000,
  headers: { "Content-Type": "application/json" },
});

// ─── Token 注入（由需要鉴权的业务模块按需调用） ─────────────────────────────

type TokenGetter = () => string;
let _getToken: TokenGetter = () => "";

/** 注入 token 来源，用于需要统一 Authorization 头的 H5 项目 */
export function setTokenGetter(getter: TokenGetter) {
  _getToken = getter;
}

// ─── 请求拦截器 ──────────────────────────────────────────────────────────────

instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = _getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// ─── 响应拦截器 ──────────────────────────────────────────────────────────────

instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // blob 请求直接透传，由调用方自行处理
    if (response.config.responseType === "blob") {
      return response as unknown as AxiosResponse;
    }

    const { code, msg, data } = response.data;

    // 业务成功码，直接返回 data
    if (code === 0 || code === 200) {
      return data as unknown as AxiosResponse;
    }

    // 登录态失效
    if (code === 401) {
      showToast("登录已过期，请重新登录");
      // 如果需要跳登录页，在此处理：window.location.href = '/login'
      return Promise.reject(new ApiError(code, msg));
    }

    // 其他业务错误
    showToast(msg || "请求失败");
    return Promise.reject(new ApiError(code, msg));
  },
  (error) => {
    if (axios.isCancel(error)) {
      // 主动取消的请求，静默处理
      return Promise.reject(error);
    }

    if (error.response) {
      // HTTP 层错误（4xx / 5xx）
      const status: number = error.response.status;
      const msgMap: Record<number, string> = {
        400: "请求参数错误",
        401: "登录已过期，请重新登录",
        403: "没有操作权限",
        404: "请求地址不存在",
        500: "服务器内部错误",
        502: "服务暂时不可用",
        503: "服务维护中，请稍后再试",
      };
      showToast(msgMap[status] ?? `请求错误（${status}）`);
    } else if (error.request) {
      // 请求已发出但没有收到响应（断网 / 超时）
      showToast("网络异常，请检查网络连接");
    } else {
      showToast("请求发送失败，请稍后重试");
    }

    return Promise.reject(error);
  },
);

// ─── 快捷方法 ────────────────────────────────────────────────────────────────

/**
 * GET 请求
 * @param url    请求路径
 * @param params query 参数
 * @param config 其他 axios 配置
 */
export function get<T = unknown>(
  url: string,
  params?: Record<string, unknown>,
  config?: AxiosRequestConfig,
): Promise<T> {
  return instance.get(url, { params, ...config });
}

/**
 * POST 请求
 * @param url    请求路径
 * @param data   请求体
 * @param config 其他 axios 配置
 */
export function post<T = unknown>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig,
): Promise<T> {
  return instance.post(url, data, config);
}

/**
 * PUT 请求
 */
export function put<T = unknown>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig,
): Promise<T> {
  return instance.put(url, data, config);
}

/**
 * DELETE 请求
 */
export function del<T = unknown>(
  url: string,
  config?: AxiosRequestConfig,
): Promise<T> {
  return instance.delete(url, config);
}

export default instance;
