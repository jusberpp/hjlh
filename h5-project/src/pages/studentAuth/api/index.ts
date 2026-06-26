import { get, post } from "@/utils/request";
import instance from "@/utils/request";

// ─── 类型定义 ─────────────────────────────────────────────────────────────────

export interface VerifyStudentReq {
  school: string;
  grade: string;
  name: string;
  studentId: string;
}

export interface VerifyStudentRes {
  id: string;
  school: string;
  grade: string;
  className: string;
  studentId: string;
  studentName: string;
  learningLevels: string;
  /** 已绑定手机号时返回，有值则跳过绑定流程直接查 bind-result */
  phone?: string;
  goodsName: string;
  resourceId: string;
  goodsImg: string;
  courseUrl: string;
  token: string;
}

export interface BindPhoneReq {
  id: string;
  phone: string;
}

export interface CourseFile {
  id: string;
  fileName: string;
  fileSize: number;
  updateTime: string;
}

/** /auth/bind-result 返回结构（学生信息 + 课程 + 文件列表） */
export interface BindResultRes {
  id: string;
  school: string;
  grade: string;
  className: string;
  studentId: string;
  studentName: string;
  learningLevels: string;
  /** 脱敏手机号，中间4位隐藏 */
  phone: string;
  goodsName: string;
  resourceId: string;
  goodsImg: string;
  courseUrl: string;
  files: CourseFile[];
}

function getStudentAuthHeaders(token: string) {
  // api.json 明确要求这些已认证接口使用名为 token 的请求头，不使用 Authorization: Bearer。
  return { token };
}

// ─── 接口方法 ─────────────────────────────────────────────────────────────────

/** 获取学校列表 */
export function fetchSchools() {
  return get<string[]>("/auth/schools");
}

/** 获取年级列表 */
export function fetchGrades() {
  return get<string[]>("/auth/grades");
}

/** 学生信息校验
 * - 返回 phone 有值：学生已绑定，直接调 fetchBindResult 展示成功页
 * - 返回 phone 无值：走确认报名 → 绑定手机号流程
 */
export function verifyStudent(data: VerifyStudentReq) {
  return post<VerifyStudentRes>("/auth/verify-student", data);
}

/** 手机号绑定，成功返回 true */
export function bindPhone(data: BindPhoneReq, token: string) {
  return post<boolean>("/auth/bind-phone", data, {
    headers: getStudentAuthHeaders(token),
  });
}

/** 查询开通结果（bind-phone 成功后 / 已绑定学生直接调用）
 * 返回完整学生信息 + 课程信息 + 资料文件列表
 */
export function fetchBindResult(token: string) {
  return get<BindResultRes>("/auth/bind-result", undefined, {
    headers: getStudentAuthHeaders(token),
  });
}

/**
 * 下载文件（axios 发请求拿 blob，支持跨域 + token 鉴权）
 * 返回 { blob, filename }，由调用方触发浏览器下载
 */
export async function downloadFile(
  fileId: string,
  token: string,
): Promise<{ blob: Blob; filename: string }> {
  const response = await instance.get(`/auth/files/${fileId}/download`, {
    headers: getStudentAuthHeaders(token),
    responseType: "blob",
  });

  // responseType='blob' 时拦截器透传原始 response
  const blob = response.data as Blob;
  const disposition: string = response.headers["content-disposition"] ?? "";

  // 从 Content-Disposition 解析文件名，形如：attachment; filename="xxx.pdf" 或 filename*=UTF-8''xxx.pdf
  const match =
    disposition.match(/filename\*=UTF-8''(.+)/i) ??
    disposition.match(/filename="?([^";\n]+)"?/i);
  const filename = match ? decodeURIComponent(match[1]) : `file_${fileId}`;

  return { blob, filename };
}
