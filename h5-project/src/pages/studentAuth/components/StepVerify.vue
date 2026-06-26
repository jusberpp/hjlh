<template>
  <div class="auth-step">
    <PageHeader title="学员身份认证" />

    <div class="auth-form">
      <!-- 学校名称 -->
      <div class="field">
        <label class="auth-field-label">学校名称</label>
        <div class="auth-control" @click="showSchoolPicker = true">
          <span :class="['auth-control-value', { placeholder: !form.school }]">
            {{ form.school || "请输入或者选择学校" }}
          </span>
          <van-icon name="arrow-down" class="auth-arrow" />
        </div>
      </div>

      <!-- 年级 -->
      <div class="field">
        <label class="auth-field-label">年级</label>
        <div class="auth-control" @click="showGradePicker = true">
          <span :class="['auth-control-value', { placeholder: !form.grade }]">
            {{ form.grade || "请选择年级" }}
          </span>
          <van-icon name="arrow-down" class="auth-arrow" />
        </div>
      </div>

      <!-- 学生姓名 -->
      <div class="field">
        <label class="auth-field-label">学生姓名</label>
        <div class="auth-control auth-control--input">
          <input
            v-model="form.name"
            class="auth-input"
            type="text"
            placeholder="请输入真实姓名"
            maxlength="20"
            @blur="validateName"
          />
          <van-icon name="arrow-down" class="auth-arrow is-hidden" />
        </div>
        <p v-if="errors.name" class="auth-error">{{ errors.name }}</p>
      </div>

      <!-- 学号 -->
      <div class="field">
        <label class="auth-field-label">学号</label>
        <div class="auth-control auth-control--input">
          <input
            v-model="form.studentId"
            class="auth-input"
            type="text"
            placeholder="请输入学号"
            maxlength="30"
            @blur="validateStudentId"
          />
          <van-icon name="arrow-down" class="auth-arrow is-hidden" />
        </div>
        <p v-if="errors.studentId" class="auth-error">{{ errors.studentId }}</p>
      </div>
    </div>

    <div class="auth-btn-wrap">
      <van-button
        class="auth-primary-btn"
        block
        :loading="loading"
        @click="handleSubmit"
      >
        验证信息
      </van-button>
    </div>

    <!-- 学校选择器 -->
    <van-popup v-model:show="showSchoolPicker" position="bottom" round>
      <van-picker
        :columns="schoolColumns"
        @confirm="onSchoolConfirm"
        @cancel="showSchoolPicker = false"
      />
    </van-popup>

    <!-- 年级选择器 -->
    <van-popup v-model:show="showGradePicker" position="bottom" round>
      <van-picker
        :columns="gradeColumns"
        @confirm="onGradeConfirm"
        @cancel="showGradePicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { showToast } from "vant";
import { fetchGrades, fetchSchools, verifyStudent } from "../api";
import type { VerifyStudentRes } from "../api";
import type { VerifyForm } from "../types";
import PageHeader from "./PageHeader.vue";

const props = defineProps<{
  initialForm?: VerifyForm | null;
}>();

const emit = defineEmits<{
  success: [data: VerifyStudentRes];
  formChange: [form: VerifyForm];
}>();

const form = reactive<VerifyForm>({
  school: "",
  grade: "",
  name: "",
  studentId: "",
});

const errors = reactive({
  name: "",
  studentId: "",
});

const loading = ref(false);
const showSchoolPicker = ref(false);
const showGradePicker = ref(false);
const schoolList = ref<string[]>([]);
const gradeList = ref<string[]>([]);

const schoolColumns = computed(() =>
  schoolList.value.map((s) => ({ text: s, value: s })),
);
const gradeColumns = computed(() =>
  gradeList.value.map((g) => ({ text: g, value: g })),
);

onMounted(async () => {
  const [schools, grades] = await Promise.all([fetchSchools(), fetchGrades()]);
  schoolList.value = schools;
  gradeList.value = grades;

  // 恢复之前填写的表单数据
  if (props.initialForm) {
    Object.assign(form, props.initialForm);
  }
});

// 监听表单变化，向父组件同步
watch(form, () => {
  emit("formChange", { ...form });
}, { deep: true });

function onSchoolConfirm({ selectedValues }: { selectedValues: string[] }) {
  form.school = selectedValues[0] ?? "";
  showSchoolPicker.value = false;
}
function onGradeConfirm({ selectedValues }: { selectedValues: string[] }) {
  form.grade = selectedValues[0] ?? "";
  showGradePicker.value = false;
}

function validateName() {
  const trimmed = form.name.trim();
  if (!trimmed) {
    errors.name = "请输入学生姓名";
    return false;
  }
  if (trimmed.length < 2) {
    errors.name = "姓名长度至少为2个字符";
    return false;
  }
  errors.name = "";
  return true;
}

function validateStudentId() {
  const trimmed = form.studentId.trim();
  if (!trimmed) {
    errors.studentId = "请输入学号";
    return false;
  }
  if (!/^\d+$/.test(trimmed)) {
    errors.studentId = "学号必须为纯数字";
    return false;
  }
  errors.studentId = "";
  return true;
}

function validateAll() {
  const isNameValid = validateName();
  const isStudentIdValid = validateStudentId();
  
  if (!form.school) {
    showToast("请选择学校");
    return false;
  }
  if (!form.grade) {
    showToast("请选择年级");
    return false;
  }
  
  return isNameValid && isStudentIdValid;
}

async function handleSubmit() {
  if (loading.value) return;
  
  if (!validateAll()) return;

  loading.value = true;
  try {
    const res = await verifyStudent({
      school: form.school,
      grade: form.grade,
      name: form.name.trim(),
      studentId: form.studentId.trim(),
    });
    emit("success", res);
  } finally {
    loading.value = false;
  }
}
</script>
