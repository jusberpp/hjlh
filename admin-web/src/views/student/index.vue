<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Download,
  Edit,
  Plus,
  Search,
  Upload,
  UploadFilled,
} from "@element-plus/icons-vue";
import {
  listStudents,
  getStudent,
  createStudent,
  updateStudent,
  downloadImportTemplate,
  validateImport,
  confirmImport,
  exportStudents,
} from "@/api/study/student";
import { courseOptions as fetchCourseOptions } from "@/api/course/course";
import { ApiError, saveBlob, toDisplayDateTime } from "@/utils/request";

const gradeOptions = ref([]);
const courseOptions = ref([]);

const filters = reactive({
  keyword: "",
  grade: "",
  courseId: "",
  phoneStatus: "",
});
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableLoading = ref(false);
const students = ref([]);

const loadOptions = async () => {
  try {
    const data = await fetchCourseOptions();
    gradeOptions.value = data.grades || [];
    courseOptions.value = data.courses || [];
  } catch (error) {
    // 静默处理，筛选仍可使用关键词
  }
};

const courseName = (courseId) => {
  const course = courseOptions.value.find((item) => item.courseId === courseId);
  return course ? course.courseName : "";
};

const loadList = async () => {
  tableLoading.value = true;
  try {
    const result = await listStudents({
      keyword: filters.keyword || undefined,
      grade: filters.grade || undefined,
      courseId: filters.courseId || undefined,
      phoneStatus: filters.phoneStatus || undefined,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    });
    students.value = result.rows || [];
    total.value = result.total || 0;
  } catch (error) {
    students.value = [];
    total.value = 0;
  } finally {
    tableLoading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  loadList();
};

const resetFilters = () => {
  Object.assign(filters, { keyword: "", grade: "", courseId: "", phoneStatus: "" });
  currentPage.value = 1;
  loadList();
};

const handleExport = async () => {
  try {
    const { blob, filename } = await exportStudents({
      keyword: filters.keyword || undefined,
      grade: filters.grade || undefined,
      courseId: filters.courseId || undefined,
      phoneStatus: filters.phoneStatus || undefined,
    });
    saveBlob(blob, filename || "学员信息.xlsx");
    ElMessage.success("导出成功");
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "导出失败");
  }
};

const dialogVisible = ref(false);
const dialogMode = ref("create");
const formRef = ref();
const submitLoading = ref(false);
const form = reactive({
  studentId: null,
  school: "",
  grade: "",
  className: "",
  studentName: "",
  studentNo: "",
  learningLevels: "",
  courseIds: [],
  primaryCourseId: null,
  authorizedPhone: "",
});

const rules = {
  school: [{ required: true, message: "请输入学校", trigger: "blur" }],
  grade: [{ required: true, message: "请选择年级", trigger: "change" }],
  className: [{ required: true, message: "请输入班级", trigger: "blur" }],
  studentName: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  studentNo: [{ required: true, message: "请输入学号", trigger: "blur" }],
  courseIds: [{ required: true, message: "请选择授权课程", trigger: "change" }],
  authorizedPhone: [
    {
      validator: (rule, value, callback) => {
        if (value && !/^1\d{10}$/.test(value)) {
          callback(new Error("请输入正确的 11 位手机号"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
};

const resetForm = () => {
  Object.assign(form, {
    studentId: null,
    school: "",
    grade: "",
    className: "",
    studentName: "",
    studentNo: "",
    learningLevels: "",
    courseIds: [],
    primaryCourseId: null,
    authorizedPhone: "",
  });
  formRef.value?.clearValidate();
};

const openCreate = () => {
  dialogMode.value = "create";
  resetForm();
  dialogVisible.value = true;
};

const openEdit = async (row) => {
  dialogMode.value = "edit";
  resetForm();
  try {
    const detail = await getStudent(row.studentId);
    Object.assign(form, {
      studentId: detail.studentId,
      school: detail.school,
      grade: detail.grade,
      className: detail.className,
      studentName: detail.studentName,
      studentNo: detail.studentNo,
      learningLevels: detail.learningLevels || "",
      courseIds: detail.courseIds || [],
      primaryCourseId: detail.primaryCourseId ?? null,
      authorizedPhone: detail.authorizedPhone || "",
    });
    dialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "加载学员详情失败");
  }
};

const saveStudent = async () => {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  if (form.primaryCourseId && !form.courseIds.includes(form.primaryCourseId)) {
    ElMessage.warning("主课程必须在授权课程中");
    return;
  }
  submitLoading.value = true;
  try {
    const payload = {
      ...form,
      learningLevels: form.learningLevels || undefined,
      authorizedPhone: form.authorizedPhone || undefined,
      primaryCourseId: form.primaryCourseId || undefined,
    };
    if (dialogMode.value === "edit") {
      await updateStudent(payload);
      ElMessage.success("学员信息已更新");
    } else {
      await createStudent(payload);
      ElMessage.success("学员新增成功");
    }
    dialogVisible.value = false;
    loadList();
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "保存失败");
  } finally {
    submitLoading.value = false;
  }
};

// Excel 批量导入
const importVisible = ref(false);
const importLoading = ref(false);
const importFile = ref(null);
const validationResult = ref(null);

const openImport = () => {
  importFile.value = null;
  validationResult.value = null;
  importVisible.value = true;
};

const handleDownloadTemplate = async () => {
  try {
    const { blob, filename } = await downloadImportTemplate();
    saveBlob(blob, filename || "学员信息导入模板.xlsx");
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "模板下载失败");
  }
};

const handleImportFile = (uploadFile) => {
  importFile.value = uploadFile.raw;
  validationResult.value = null;
};

const handleImportRemove = () => {
  importFile.value = null;
  validationResult.value = null;
};

const handleValidate = async () => {
  if (!importFile.value) {
    ElMessage.warning("请先选择 Excel 文件");
    return;
  }
  importLoading.value = true;
  try {
    const result = await validateImport(importFile.value);
    validationResult.value = result;
    if (result.canImport) {
      ElMessage.success(`校验通过 ${result.validCount} 条`);
    } else if (result.invalidCount > 0) {
      ElMessage.warning(`存在 ${result.invalidCount} 条错误，请修正后重新上传`);
    } else {
      ElMessage.warning("文件中没有可导入的有效数据");
    }
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "校验失败");
  } finally {
    importLoading.value = false;
  }
};

const updateSupport = ref(false);

const handleConfirmImport = async () => {
  if (!validationResult.value || !validationResult.value.canImport) {
    ElMessage.warning("请先完成文件校验");
    return;
  }
  try {
    await ElMessageBox.confirm(
      `校验通过 ${validationResult.value.validCount} 条${
        validationResult.value.emptyPhoneCount
          ? `，其中 ${validationResult.value.emptyPhoneCount} 条手机号为空`
          : ""
      }。确认导入学员信息吗？`,
      "确认批量导入",
      { type: "warning", confirmButtonText: "确认导入" },
    );
  } catch {
    return;
  }
  importLoading.value = true;
  try {
    const result = await confirmImport({
      importToken: validationResult.value.importToken,
      updateSupport: updateSupport.value,
    });
    ElMessage.success(`已成功导入 ${result.successCount} 条（更新 ${result.updateCount} 条）`);
    importVisible.value = false;
    importFile.value = null;
    validationResult.value = null;
    loadList();
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "导入失败");
  } finally {
    importLoading.value = false;
  }
};

onMounted(() => {
  loadOptions();
  loadList();
});
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <h1>学员信息管理</h1>
        <p>维护学校、年级、班级、课程授权及手机号绑定信息。</p>
      </div>
      <div class="page-actions">
        <el-button :icon="Upload" @click="openImport">Excel 批量导入</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增学员</el-button>
      </div>
    </div>

    <div class="content-card filter-panel">
      <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="姓名 / 学号 / 学校 / 手机号"
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="filters.grade" clearable placeholder="全部年级" style="width: 130px">
            <el-option
              v-for="item in gradeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="授权课程">
          <el-select v-model="filters.courseId" clearable placeholder="全部课程" style="width: 200px">
            <el-option
              v-for="item in courseOptions"
              :key="item.courseId"
              :label="item.courseName"
              :value="item.courseId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-select v-model="filters.phoneStatus" clearable placeholder="全部状态" style="width: 130px">
            <el-option label="已授权" value="BOUND" />
            <el-option label="未授权" value="UNBOUND" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card table-card">
      <div class="table-toolbar">
        <strong>学员列表 <span>共 {{ total }} 条</span></strong>
        <el-button text type="primary" :icon="Download" @click="handleExport">导出查询结果</el-button>
      </div>
      <el-table v-loading="tableLoading" :data="students" border stripe>
        <el-table-column prop="school" label="学校" min-width="180" show-overflow-tooltip />
        <el-table-column prop="gradeName" label="年级" width="76" align="center" />
        <el-table-column prop="className" label="班级" width="78" align="center" />
        <el-table-column prop="studentName" label="姓名" width="90" />
        <el-table-column prop="studentNo" label="学号" min-width="125" />
        <el-table-column label="授权课程" min-width="220">
          <template #default="{ row }">
            <el-tag
              v-for="course in row.courses"
              :key="course.courseId"
              size="small"
              :effect="course.primary ? 'dark' : 'plain'"
              style="margin: 2px 5px 2px 0"
            >
              {{ course.courseName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="授权手机号" width="135">
          <template #default="{ row }">
            <div v-if="row.phoneMasked" class="phone-cell">
              <span>{{ row.phoneMasked }}</span>
              <small>{{ row.phoneSource }}</small>
            </div>
            <el-tag v-else type="warning" size="small">未授权</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="150">
          <template #default="{ row }">{{ toDisplayDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @current-change="loadList"
          @size-change="loadList"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增学员' : '编辑学员'"
      width="680px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="18">
          <el-col :span="16">
            <el-form-item label="学校" prop="school">
              <el-input v-model="form.school" placeholder="请输入学校全称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年级" prop="grade">
              <el-select v-model="form.grade" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="item in gradeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班级" prop="className">
              <el-input v-model="form.className" placeholder="如：3班" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="studentName">
              <el-input v-model="form.studentName" placeholder="请输入学员姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="form.studentNo" placeholder="请输入学号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="授权手机号" prop="authorizedPhone">
              <el-input v-model="form.authorizedPhone" maxlength="11" placeholder="选填，11 位手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="授权课程" prop="courseIds">
              <el-select
                v-model="form.courseIds"
                multiple
                collapse-tags
                collapse-tags-tooltip
                placeholder="可选择多个课程"
                style="width: 100%"
              >
                <el-option
                  v-for="item in courseOptions"
                  :key="item.courseId"
                  :label="item.courseName"
                  :value="item.courseId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主课程">
              <el-select
                v-model="form.primaryCourseId"
                clearable
                placeholder="默认取第一门"
                style="width: 100%"
              >
                <el-option
                  v-for="item in courseOptions.filter((c) => form.courseIds.includes(c.courseId))"
                  :key="item.courseId"
                  :label="item.courseName"
                  :value="item.courseId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学习分层">
              <el-input v-model="form.learningLevels" placeholder="选填，兼容旧 H5 字段" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="saveStudent">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="Excel 模板批量导入" width="640px">
      <div class="template-tip">
        <div>
          <strong>第一步：下载并填写标准模板</strong>
          <p>请勿修改表头，授权课程使用课程名称，多门课程用中文顿号分隔。</p>
        </div>
        <el-button :icon="Download" @click="handleDownloadTemplate">下载模板</el-button>
      </div>
      <el-upload
        drag
        accept=".xlsx,.xls"
        :auto-upload="false"
        :limit="1"
        :on-change="handleImportFile"
        :on-remove="handleImportRemove"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖放 Excel 文件到这里，或 <em>点击选择文件</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 xlsx、xls，单次最多导入 2,000 条。</div>
        </template>
      </el-upload>
      <div v-if="validationResult" class="validation-result">
        <div class="validation-summary">
          <div>
            <strong>校验结果</strong>
            <p>
              共 {{ validationResult.totalCount }} 条，有效
              {{ validationResult.validCount }} 条，错误
              {{ validationResult.invalidCount }} 条<template v-if="validationResult.emptyPhoneCount">
                ，其中 {{ validationResult.emptyPhoneCount }} 条手机号为空</template>
            </p>
          </div>
          <el-tag :type="validationResult.canImport ? 'success' : 'danger'">
            {{ validationResult.canImport ? "可导入" : "不可导入" }}
          </el-tag>
        </div>
        <el-table
          v-if="validationResult.errors && validationResult.errors.length"
          :data="validationResult.errors"
          border
          max-height="200"
          size="small"
          style="margin-top: 12px"
        >
          <el-table-column prop="rowNum" label="行号" width="70" align="center" />
          <el-table-column prop="field" label="字段" width="120" />
          <el-table-column prop="value" label="原值" min-width="120" show-overflow-tooltip />
          <el-table-column prop="message" label="错误原因" min-width="160" />
        </el-table>
        <div v-if="validationResult.canImport" class="confirm-options">
          <el-checkbox v-model="updateSupport">已存在学号则更新（否则跳过并报错）</el-checkbox>
        </div>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button :loading="importLoading" @click="handleValidate">校验文件</el-button>
        <el-button
          type="primary"
          :loading="importLoading"
          :disabled="!validationResult || !validationResult.canImport"
          @click="handleConfirmImport"
        >
          确认导入
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.phone-cell {
  display: grid;
}

.phone-cell small {
  margin-top: 3px;
  color: #98a2b3;
  font-size: 10px;
}

.template-tip,
.validation-result {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 18px;
  padding: 14px;
  border: 1px solid #d9e8fb;
  border-radius: 6px;
  background: #f6faff;
}

.template-tip {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.template-tip p,
.validation-result p {
  margin: 5px 0 0;
  color: #718096;
  font-size: 12px;
  line-height: 1.55;
}

.validation-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.validation-result {
  border-color: #ccebd7;
  background: #f3fbf6;
}

.validation-result .el-tag {
  margin-left: auto;
}

.confirm-options {
  padding-top: 4px;
}
</style>
