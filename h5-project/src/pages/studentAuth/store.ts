import { defineStore } from "pinia";
import { ref } from "vue";
import type { BindResultRes, VerifyStudentRes } from "./api";
import type { VerifyForm } from "./types";

export const useAuthStore = defineStore(
  "studentAuth",
  () => {
    const token = ref("");
    const studentInfo = ref<VerifyStudentRes | null>(null);
    const bindResult = ref<BindResultRes | null>(null);
    const savedVerifyForm = ref<VerifyForm | null>(null);

    function setToken(value: string) {
      token.value = value;
    }

    function clearToken() {
      token.value = "";
    }

    function setStudentInfo(info: VerifyStudentRes) {
      studentInfo.value = info;
      if (info.token) {
        setToken(info.token);
      }
    }

    function clearStudentInfo() {
      studentInfo.value = null;
    }

    function setBindResult(result: BindResultRes) {
      bindResult.value = result;
    }

    function clearBindResult() {
      bindResult.value = null;
    }

    function setSavedVerifyForm(form: VerifyForm) {
      savedVerifyForm.value = form;
    }

    function clearSavedVerifyForm() {
      savedVerifyForm.value = null;
    }

    function resetAll() {
      clearToken();
      clearStudentInfo();
      clearBindResult();
      clearSavedVerifyForm();
    }

    return {
      token,
      studentInfo,
      bindResult,
      savedVerifyForm,
      setToken,
      clearToken,
      setStudentInfo,
      clearStudentInfo,
      setBindResult,
      clearBindResult,
      setSavedVerifyForm,
      clearSavedVerifyForm,
      resetAll,
    };
  },
  {
    persist: {
      key: "studentAuth",
      storage: localStorage,
    },
  },
);
