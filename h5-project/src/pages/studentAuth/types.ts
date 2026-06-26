import type { BindResultRes, VerifyStudentRes } from "./api";

// 页面步骤枚举
export type Step = "verify" | "confirm" | "bind-phone" | "success";

/** 学生身份验证表单，用于返回时恢复填写内容 */
export interface VerifyForm {
  school: string;
  grade: string;
  name: string;
  studentId: string;
}

// 跨步骤共享状态
export interface AuthState {
  step: Step;
  /** verify-student 返回的基础学生信息（含 token） */
  studentInfo: VerifyStudentRes | null;
  /** bind-result 返回的完整结果（success 页专用） */
  bindResult: BindResultRes | null;
  /** 返回 verify 步骤时恢复的表单数据 */
  savedVerifyForm: VerifyForm | null;
}
