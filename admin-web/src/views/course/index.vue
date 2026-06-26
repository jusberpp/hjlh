<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Delete,
  Edit,
  Link,
  Plus,
  Search,
  UserFilled,
  VideoPause,
  VideoPlay,
} from "@element-plus/icons-vue";
import {
  courseOptions,
  listCourses,
  getCourse,
  createCourse,
  updateCourse,
  updateCourseStatus,
  deleteCourses,
  uploadLecturerAvatar,
} from "@/api/course/course";
import { ApiError, toBackendDateTime, toDisplayDateTime } from "@/utils/request";

const grades = ref([]);
const subjects = ref([]);
const filters = reactive({ grade: "", subject: "", keyword: "", status: "" });

const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const tableLoading = ref(false);
const courses = ref([]);

const loadOptions = async () => {
  try {
    const data = await courseOptions();
    grades.value = data.grades || [];
    subjects.value = data.subjects || [];
  } catch (error) {
    // 静默
  }
};

const loadList = async () => {
  tableLoading.value = true;
  try {
    const result = await listCourses({
      grade: filters.grade || undefined,
      subject: filters.subject || undefined,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    });
    courses.value = result.rows || [];
    total.value = result.total || 0;
  } catch (error) {
    courses.value = [];
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
  Object.assign(filters, { grade: "", subject: "", keyword: "", status: "" });
  currentPage.value = 1;
  loadList();
};

const subjectLabel = (value) => {
  const item = subjects.value.find((s) => s.value === value);
  return item ? item.label : value;
};

const gradeLabel = (value) => {
  const item = grades.value.find((g) => g.value === value);
  return item ? item.label : value;
};

const toggleStatus = async (row) => {
  const next = row.status === "ENABLED" ? "DISABLED" : "ENABLED";
  const action = next === "ENABLED" ? "启用" : "停用";
  try {
    await ElMessageBox.confirm(`确认${action}课程“${row.courseName}”吗？`, `${action}课程`, {
      type: "warning",
      confirmButtonText: action,
    });
  } catch {
    return;
  }
  try {
    await updateCourseStatus({ courseId: row.courseId, status: next });
    ElMessage.success(`已${action}`);
    loadList();
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : `${action}失败`);
  }
};

const removeCourse = async (row) => {
  try {
    await ElMessageBox.confirm(
      `删除“${row.courseName}”后不可恢复，若存在学员授权、资料或提分宝关联将拒绝删除。确认删除吗？`,
      "删除课程",
      { type: "warning", confirmButtonText: "确认删除" },
    );
  } catch {
    return;
  }
  try {
    await deleteCourses(row.courseId);
    ElMessage.success("课程已删除");
    loadList();
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "删除失败");
  }
};

const dialogVisible = ref(false);
const dialogMode = ref("create");
const submitLoading = ref(false);
const avatarUploading = ref(false);
const formRef = ref();
const form = reactive({
  courseId: null,
  grade: "",
  subject: "",
  source: "",
  lecturerName: "",
  lecturerAvatarUrl: "",
  classTimes: [],
});

const rules = {
  grade: [{ required: true, message: "请选择年级", trigger: "change" }],
  subject: [{ required: true, message: "请选择学科名称", trigger: "change" }],
  source: [{ required: true, message: "请粘贴课程链接或课程 ID", trigger: "blur" }],
  lecturerName: [{ required: true, message: "请输入讲师姓名", trigger: "blur" }],
};

const resetForm = () => {
  Object.assign(form, {
    courseId: null,
    grade: "",
    subject: "",
    source: "",
    lecturerName: "",
    lecturerAvatarUrl: "",
    classTimes: [],
  });
  formRef.value?.clearValidate();
};

const openCreate = () => {
  dialogMode.value = "create";
  resetForm();
  form.classTimes = [[]];
  dialogVisible.value = true;
};

const openEdit = async (row) => {
  dialogMode.value = "edit";
  resetForm();
  try {
    const detail = await getCourse(row.courseId);
    Object.assign(form, {
      courseId: detail.courseId,
      grade: detail.grade,
      subject: detail.subject,
      source: detail.sourceValue,
      lecturerName: detail.lecturerName,
      lecturerAvatarUrl: detail.lecturerAvatarUrl || "",
      classTimes: (detail.classTimes || []).map((item) => [
        toDisplayDateTime(item.startTime),
        toDisplayDateTime(item.endTime),
      ]),
    });
    if (form.classTimes.length === 0) form.classTimes = [[]];
    dialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "加载课程详情失败");
  }
};

const addClassTime = () => {
  form.classTimes.push([]);
};

const removeClassTime = (index) => {
  if (form.classTimes.length === 1) {
    ElMessage.warning("至少保留一组上课时间");
    return;
  }
  form.classTimes.splice(index, 1);
};

const validateAvatarFile = (rawFile) => {
  const accepted = ["image/jpeg", "image/png", "image/webp"];
  if (!accepted.includes(rawFile.type)) {
    ElMessage.error("讲师头像仅支持 JPG、PNG 或 WebP 格式");
    return false;
  }
  if (rawFile.size > 2 * 1024 * 1024) {
    ElMessage.error("讲师头像大小不能超过 2MB");
    return false;
  }
  return true;
};

const handleAvatarChange = async (uploadFile) => {
  const rawFile = uploadFile.raw;
  if (!rawFile || !validateAvatarFile(rawFile)) return;
  avatarUploading.value = true;
  try {
    const data = await uploadLecturerAvatar(rawFile);
    form.lecturerAvatarUrl = data.lecturerAvatarUrl;
    ElMessage.success("头像上传成功");
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "头像上传失败");
  } finally {
    avatarUploading.value = false;
  }
};

const removeAvatar = () => {
  form.lecturerAvatarUrl = "";
};

const saveCourse = async () => {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  if (form.classTimes.some((timeRange) => timeRange.length !== 2)) {
    ElMessage.warning("请完整设置每一组上课开始和结束时间");
    return;
  }
  if (
    form.classTimes.some(
      ([startTime, endTime]) => new Date(startTime) >= new Date(endTime),
    )
  ) {
    ElMessage.warning("上课结束时间必须晚于开始时间");
    return;
  }
  const sortedClassTimes = form.classTimes
    .map(([startTime, endTime]) => ({
      start: new Date(startTime).getTime(),
      end: new Date(endTime).getTime(),
    }))
    .sort((left, right) => left.start - right.start);
  if (
    sortedClassTimes.some(
      (timeRange, index) =>
        index > 0 && timeRange.start < sortedClassTimes[index - 1].end,
    )
  ) {
    ElMessage.warning("上课时间存在重复或重叠，请调整后保存");
    return;
  }

  const payload = {
    grade: form.grade,
    subject: form.subject,
    lecturerName: form.lecturerName.trim(),
    lecturerAvatarUrl: form.lecturerAvatarUrl || undefined,
    sourceValue: form.source.trim(),
    classTimes: form.classTimes.map(([startTime, endTime]) => ({
      startTime: toBackendDateTime(startTime),
      endTime: toBackendDateTime(endTime),
    })),
  };
  if (dialogMode.value === "edit") payload.courseId = form.courseId;

  submitLoading.value = true;
  try {
    if (dialogMode.value === "edit") {
      await updateCourse(payload);
      ElMessage.success("课程配置已更新");
    } else {
      await createCourse(payload);
      ElMessage.success("课程创建成功");
    }
    dialogVisible.value = false;
    loadList();
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "保存失败");
  } finally {
    submitLoading.value = false;
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
        <h1>课程管理</h1>
        <p>配置年级、学科与小鹅通课程链接或课程 ID 的映射关系。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增课程</el-button>
    </div>

    <div class="content-card filter-panel">
      <el-form :inline="true" class="filter-form" @submit.prevent="handleSearch">
        <el-form-item label="年级">
          <el-select v-model="filters.grade" clearable placeholder="全部年级" style="width: 140px">
            <el-option v-for="item in grades" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="学科">
          <el-select v-model="filters.subject" clearable placeholder="全部学科" style="width: 150px">
            <el-option v-for="item in subjects" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 120px">
            <el-option label="已启用" value="ENABLED" />
            <el-option label="已停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程信息">
          <el-input v-model="filters.keyword" clearable placeholder="课程名 / ID / 链接" style="width: 240px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="content-card table-card">
      <div class="table-toolbar">
        <strong>课程列表 <span>共 {{ total }} 门</span></strong>
      </div>
      <el-table v-loading="tableLoading" :data="courses" border stripe>
        <el-table-column label="课程" min-width="220">
          <template #default="{ row }">
            <div class="course-cell">
              <span
                class="subject-badge"
                :class="{ physics: row.subject === 'PHYSICS', english: row.subject === 'ENGLISH', chinese: row.subject === 'CHINESE' }"
              >
                {{ subjectLabel(row.subject).slice(0, 1) }}
              </span>
              <div class="course-cell-copy">
                <strong>{{ row.courseName }}</strong>
                <span>{{ row.externalCourseId }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="授课讲师" min-width="140">
          <template #default="{ row }">
            <div class="lecturer-cell">
              <el-avatar :size="34" :src="row.lecturerAvatarUrl">
                {{ row.lecturerName ? row.lecturerName.slice(0, 1) : "" }}
              </el-avatar>
              <strong>{{ row.lecturerName }}</strong>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="小鹅通课程地址 / ID" min-width="280">
          <template #default="{ row }">
            <div class="source-cell">
              <el-icon><Link /></el-icon>
              <span>{{ row.sourceValue }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="上课时间" min-width="170">
          <template #default="{ row }">
            <div class="class-time-cell">
              <strong>{{ row.classTimes.length }} 个课次</strong>
              <span v-if="row.classTimes.length">{{ toDisplayDateTime(row.classTimes[0].startTime) }} 起</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="studentCount" label="授权学员" width="92" align="center" />
        <el-table-column prop="materialCount" label="资料数" width="82" align="center" />
        <el-table-column prop="treasureBatchCount" label="提分宝" width="82" align="center" />
        <el-table-column label="状态" width="92" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ row.status === "ENABLED" ? "已启用" : "已停用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="150">
          <template #default="{ row }">{{ toDisplayDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 'ENABLED' ? 'warning' : 'success'"
              :icon="row.status === 'ENABLED' ? VideoPause : VideoPlay"
              @click="toggleStatus(row)"
            >
              {{ row.status === "ENABLED" ? "停用" : "启用" }}
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="removeCourse(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          @current-change="loadList"
          @size-change="loadList"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增课程' : '编辑课程'"
      width="680px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
        <el-form-item label="年级" prop="grade">
          <el-select v-model="form.grade" placeholder="请选择年级" style="width: 100%">
            <el-option v-for="item in grades" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="学科名称" prop="subject">
          <el-select v-model="form.subject" placeholder="请选择学科" style="width: 100%">
            <el-option v-for="item in subjects" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="讲师姓名" prop="lecturerName">
          <el-input v-model="form.lecturerName" maxlength="30" show-word-limit placeholder="请输入授课讲师姓名" />
        </el-form-item>
        <el-form-item label="讲师头像">
          <div class="avatar-upload-row">
            <el-upload
              class="avatar-uploader"
              accept=".jpg,.jpeg,.png,.webp"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleAvatarChange"
            >
              <img
                v-if="form.lecturerAvatarUrl"
                :src="form.lecturerAvatarUrl"
                class="avatar-preview"
                alt="讲师头像预览"
              />
              <el-icon v-else class="avatar-uploader-icon"><UserFilled /></el-icon>
            </el-upload>
            <div class="avatar-upload-tip">
              <span>支持 JPG、PNG、WebP，文件不超过 2MB。上传后自动保存到正式存储。</span>
              <el-button
                v-if="form.lecturerAvatarUrl"
                link
                type="danger"
                :icon="Delete"
                @click="removeAvatar"
              >
                移除头像
              </el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="课程链接 / ID" prop="source">
          <el-input
            v-model="form.source"
            type="textarea"
            :rows="3"
            placeholder="粘贴小鹅通课程地址，或直接输入课程 ID"
          />
        </el-form-item>
        <el-form-item label="上课时间" required>
          <div class="class-time-editor">
            <div
              v-for="(_, index) in form.classTimes"
              :key="index"
              class="class-time-row"
            >
              <span class="class-time-index">第 {{ index + 1 }} 次</span>
              <el-date-picker
                v-model="form.classTimes[index]"
                type="datetimerange"
                value-format="YYYY-MM-DD HH:mm"
                format="YYYY-MM-DD HH:mm"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                style="width: 390px"
              />
              <el-button link type="danger" :icon="Delete" @click="removeClassTime(index)">移除</el-button>
            </div>
            <el-button plain type="primary" :icon="Plus" @click="addClassTime">添加上课时间</el-button>
          </div>
        </el-form-item>
        <el-alert
          title="保存时后端会解析小鹅通课程 ID 并校验课次不重叠。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="saveCourse">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.source-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 7px;
  color: #53647d;
}

.source-cell .el-icon {
  flex: 0 0 auto;
  color: #1677ff;
}

.source-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lecturer-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.lecturer-cell strong {
  color: #344054;
  font-size: 13px;
}

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-uploader {
  width: 82px;
  height: 82px;
}

.avatar-uploader :deep(.el-upload) {
  display: grid;
  width: 82px;
  height: 82px;
  place-items: center;
  overflow: hidden;
  border: 1px dashed #b8c4d3;
  border-radius: 8px;
  background: #f8fafc;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #1677ff;
  background: #f2f7ff;
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-uploader-icon {
  color: #7f8da1;
  font-size: 28px;
}

.avatar-upload-tip {
  display: grid;
  justify-items: start;
  gap: 5px;
}

.avatar-upload-tip span {
  color: #98a2b3;
  font-size: 12px;
}

.class-time-cell {
  display: grid;
}

.class-time-cell strong {
  font-size: 12px;
}

.class-time-cell span {
  margin-top: 3px;
  color: #98a2b3;
  font-size: 11px;
}

.class-time-editor {
  display: grid;
  width: 100%;
  gap: 10px;
}

.class-time-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.class-time-index {
  min-width: 48px;
  color: #667085;
  font-size: 12px;
}

.class-time-editor > .el-button {
  width: 142px;
}
</style>
