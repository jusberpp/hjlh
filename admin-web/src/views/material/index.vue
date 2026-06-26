<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  ArrowLeft,
  Delete,
  Download,
  Edit,
  Files,
  Plus,
  Search,
  UploadFilled,
} from "@element-plus/icons-vue";
import {
  materialCourseList,
  listMaterials,
  uploadMaterials,
  createMaterialBatch,
  getMaterial,
  updateMaterial,
  deleteMaterials,
  exportSubmissionScores,
} from "@/api/course/material";
import {
  homeworkOptions,
  listTreasures,
  parseTreasure,
  confirmTreasure,
  deleteTreasures,
} from "@/api/course/treasure";
import {
  ApiError,
  saveBlob,
  toBackendDateTime,
  toDisplayDateTime,
} from "@/utils/request";
import { formatFileSize, upperExtension } from "@/utils/format";

const keyword = ref("");
const selectedCourse = ref(null);
const activeTab = ref("materials");
const courseLoading = ref(false);
const courses = ref([]);

const visibleCourses = ref([]);

const courseTitle = ref("");
const isPhysics = ref(false);

const materials = ref([]);
const materialLoading = ref(false);
const materialTypeFilter = ref("");

const treasureBatches = ref([]);
const treasureLoading = ref(false);
const treasureHomeworkList = ref([]);

const loadCourseList = async () => {
  courseLoading.value = true;
  try {
    const data = await materialCourseList(keyword.value ? { keyword: keyword.value } : undefined);
    courses.value = data || [];
    refreshVisibleCourses();
  } catch (error) {
    courses.value = [];
    refreshVisibleCourses();
  } finally {
    courseLoading.value = false;
  }
};

const refreshVisibleCourses = () => {
  const value = keyword.value.trim().toLowerCase();
  visibleCourses.value = courses.value.filter((course) =>
    `${course.gradeName}${course.subjectName}${course.courseName}`.toLowerCase().includes(value),
  );
};

const onKeywordChange = () => {
  refreshVisibleCourses();
};

const enterCourse = (course) => {
  selectedCourse.value = course;
  courseTitle.value = `${course.gradeName} · ${course.subjectName}`;
  isPhysics.value = course.subject === "PHYSICS";
  activeTab.value = "materials";
  loadMaterials();
  loadTreasures();
  loadTreasureOptions();
};

const backToCourses = () => {
  selectedCourse.value = null;
  courseTitle.value = "";
  loadCourseList();
};

const loadMaterials = async () => {
  if (!selectedCourse.value) return;
  materialLoading.value = true;
  try {
    const data = await listMaterials({
      courseId: selectedCourse.value.courseId,
      materialType: materialTypeFilter.value || undefined,
    });
    materials.value = data || [];
  } catch (error) {
    materials.value = [];
  } finally {
    materialLoading.value = false;
  }
};

const loadTreasures = async () => {
  if (!selectedCourse.value) return;
  treasureLoading.value = true;
  try {
    const result = await listTreasures({
      courseId: selectedCourse.value.courseId,
      pageNum: 1,
      pageSize: 100,
    });
    treasureBatches.value = result.rows || [];
  } catch (error) {
    treasureBatches.value = [];
  } finally {
    treasureLoading.value = false;
  }
};

const loadTreasureOptions = async () => {
  if (!selectedCourse.value) return;
  try {
    treasureHomeworkList.value = await homeworkOptions(selectedCourse.value.courseId);
  } catch (error) {
    treasureHomeworkList.value = [];
  }
};

// ====== 资料新增 / 编辑 ======
const materialVisible = ref(false);
const materialMode = ref("create");
const editingMaterialId = ref(null);
const materialFormRef = ref();
const materialSubmitting = ref(false);
const materialForm = reactive({
  materialType: "HANDOUT",
  files: [],
});

const makeUploadItem = (rawFile, uploadInfo) => ({
  uid: rawFile.uid || `${Date.now()}-${Math.random()}`,
  raw: rawFile,
  name: uploadInfo?.fileName || rawFile.name,
  sizeText: uploadInfo?.fileSizeText || formatFileSize(rawFile.size),
  uploadToken: uploadInfo?.uploadToken || null,
  openAt: "",
  questionCount: 5,
  scores: [4, 4, 4, 4, 4],
  submitDeadline: "",
});

const resetMaterialForm = () => {
  materialForm.materialType = "HANDOUT";
  materialForm.files = [];
  editingMaterialId.value = null;
};

const openMaterialCreate = () => {
  materialMode.value = "create";
  resetMaterialForm();
  materialVisible.value = true;
};

const handleMaterialFiles = (uploadFile, uploadFiles) => {
  materialForm.files = uploadFiles.map((file) => {
    const existing = materialForm.files.find((item) => item.uid === file.uid);
    return existing || makeUploadItem(file.raw);
  });
};

const removeUploadItem = (uid) => {
  materialForm.files = materialForm.files.filter((item) => item.uid !== uid);
};

const updateQuestionCount = (file) => {
  const count = Number(file.questionCount) || 1;
  const previous = [...file.scores];
  file.scores = Array.from({ length: count }, (_, index) => previous[index] ?? 4);
};

const openMaterialEdit = async (row) => {
  materialMode.value = "edit";
  editingMaterialId.value = row.materialId;
  resetMaterialForm();
  materialForm.materialType = row.materialType;
  const scores = (row.questionScores || []).map((item) => Number(item.score));
  materialForm.files = [
    {
      uid: row.materialId,
      name: row.fileName,
      sizeText: row.fileSizeText || formatFileSize(row.fileSize),
      uploadToken: null,
      openAt: toDisplayDateTime(row.openTime),
      questionCount: row.questionCount || scores.length || 5,
      scores: scores.length ? scores : [4, 4, 4, 4, 4],
      submitDeadline: toDisplayDateTime(row.submitDeadline),
    },
  ];
  materialVisible.value = true;
};

const saveMaterials = async () => {
  if (!materialForm.files.length) {
    ElMessage.warning("请先选择需要上传的资料文件");
    return;
  }
  if (materialForm.files.some((file) => !file.openAt)) {
    ElMessage.warning("请为每个文件设置开放时间");
    return;
  }
  const isPhysicsHomework = isPhysics.value && materialForm.materialType === "HOMEWORK";
  if (isPhysicsHomework && materialForm.files.some((file) => !file.submitDeadline)) {
    ElMessage.warning("请为每个物理作业设置提交截止时间");
    return;
  }
  if (
    isPhysicsHomework &&
    materialForm.files.some((file) => new Date(file.submitDeadline) <= new Date(file.openAt))
  ) {
    ElMessage.warning("提交截止时间必须晚于资料开放时间");
    return;
  }

  materialSubmitting.value = true;
  try {
    if (materialMode.value === "edit") {
      const file = materialForm.files[0];
      const payload = {
        materialId: editingMaterialId.value,
        materialType: materialForm.materialType,
        openTime: toBackendDateTime(file.openAt),
        submitDeadline: isPhysicsHomework ? toBackendDateTime(file.submitDeadline) : undefined,
        questionCount: isPhysicsHomework ? Number(file.questionCount) : undefined,
        questionScores: isPhysicsHomework
          ? file.scores.map((score, index) => ({
              questionNo: index + 1,
              score: Number(score),
            }))
          : undefined,
      };
      await updateMaterial(payload);
      ElMessage.success("资料信息已更新");
    } else {
      // 上传所有新文件，获取一次性令牌
      const rawFiles = materialForm.files.map((item) => item.raw).filter(Boolean);
      let uploaded = [];
      if (rawFiles.length) {
        uploaded = await uploadMaterials(rawFiles);
        // 按顺序匹配 token 到文件项
        let tokenIndex = 0;
        materialForm.files.forEach((item) => {
          if (item.raw && uploaded[tokenIndex]) {
            item.uploadToken = uploaded[tokenIndex].uploadToken;
            tokenIndex++;
          }
        });
      }
      const batchPayload = {
        courseId: selectedCourse.value.courseId,
        materialType: materialForm.materialType,
        files: materialForm.files.map((file) => ({
          uploadToken: file.uploadToken,
          openTime: toBackendDateTime(file.openAt),
          submitDeadline: isPhysicsHomework ? toBackendDateTime(file.submitDeadline) : undefined,
          questionCount: isPhysicsHomework ? Number(file.questionCount) : undefined,
          questionScores: isPhysicsHomework
            ? file.scores.map((score, index) => ({
                questionNo: index + 1,
                score: Number(score),
              }))
            : undefined,
        })),
      };
      const result = await createMaterialBatch(batchPayload);
      ElMessage.success(`已新增 ${result.successCount} 个资料文件`);
    }
    materialVisible.value = false;
    loadMaterials();
    loadCourseList();
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "保存失败");
  } finally {
    materialSubmitting.value = false;
  }
};

const removeMaterial = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除资料“${row.fileName}”吗？`, "删除资料", {
      type: "warning",
      confirmButtonText: "确认删除",
    });
  } catch {
    return;
  }
  try {
    await deleteMaterials(row.materialId);
    ElMessage.success("资料已删除");
    loadMaterials();
    loadCourseList();
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "删除失败");
  }
};

const exportSubmissions = async (row) => {
  try {
    const { blob, filename } = await exportSubmissionScores(row.materialId);
    saveBlob(blob, filename || `${row.fileName}-小题分提交数据.xlsx`);
    ElMessage.success("已生成小题分提交数据");
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "导出失败");
  }
};

// ====== 提分宝 ======
const treasureVisible = ref(false);
const treasureForm = reactive({
  homeworkMaterialId: null,
  fileName: "",
  file: null,
});
const parseStatus = ref("idle");
const parseResult = ref(null);
const treasureSubmitting = ref(false);

const openTreasureCreate = () => {
  Object.assign(treasureForm, { homeworkMaterialId: null, fileName: "", file: null });
  parseStatus.value = "idle";
  parseResult.value = null;
  loadTreasureOptions();
  treasureVisible.value = true;
};

const handleTreasureFile = (file) => {
  treasureForm.file = file.raw;
  treasureForm.fileName = file.name;
  parseStatus.value = "idle";
  parseResult.value = null;
};

const handleTreasureRemove = () => {
  treasureForm.file = null;
  treasureForm.fileName = "";
  parseStatus.value = "idle";
  parseResult.value = null;
};

const handleParse = async () => {
  if (!treasureForm.file) {
    ElMessage.warning("请先选择 ZIP 压缩包");
    return;
  }
  if (!treasureForm.homeworkMaterialId) {
    ElMessage.warning("请先选择关联作业");
    return;
  }
  parseStatus.value = "parsing";
  try {
    const result = await parseTreasure(
      selectedCourse.value.courseId,
      treasureForm.homeworkMaterialId,
      treasureForm.file,
    );
    parseResult.value = result;
    parseStatus.value = result.canSubmit ? "success" : "failed";
    if (result.canSubmit) {
      ElMessage.success(result.message || "解析成功");
    } else {
      ElMessage.warning(result.message || "解析失败");
    }
  } catch (error) {
    parseStatus.value = "failed";
    parseResult.value = null;
    ElMessage.error(error instanceof ApiError ? error.message : "解析失败");
  }
};

const saveTreasure = async () => {
  if (!treasureForm.homeworkMaterialId) {
    ElMessage.warning("请选择需要上传提分宝的作业");
    return;
  }
  if (parseStatus.value !== "success" || !parseResult.value) {
    ElMessage.warning("只有解析成功的压缩包可以上传");
    return;
  }
  treasureSubmitting.value = true;
  try {
    const result = await confirmTreasure({
      courseId: selectedCourse.value.courseId,
      homeworkMaterialId: treasureForm.homeworkMaterialId,
      parseToken: parseResult.value.parseToken,
    });
    ElMessage.success(`提分宝批次上传成功，共 ${result.parsedFileCount} 个个人文件`);
    treasureVisible.value = false;
    loadTreasures();
    loadTreasureOptions();
    loadCourseList();
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "上传失败");
  } finally {
    treasureSubmitting.value = false;
  }
};

const removeBatch = async (row) => {
  try {
    await ElMessageBox.confirm(
      `删除作业“${row.homeworkTitle}”的提分宝后不可恢复，确认删除吗？`,
      "删除提分宝",
      { type: "warning", confirmButtonText: "确认删除" },
    );
  } catch {
    return;
  }
  try {
    await deleteTreasures(row.batchId);
    ElMessage.success("批次已删除");
    loadTreasures();
    loadTreasureOptions();
    loadCourseList();
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : "删除失败");
  }
};

onMounted(() => {
  loadCourseList();
});
</script>

<template>
  <section>
    <template v-if="!selectedCourse">
      <div class="page-heading">
        <div>
          <h1>课程资料管理</h1>
          <p>选择课程后管理讲义、作业、小题分设置和提分宝上传批次。</p>
        </div>
      </div>

      <div class="content-card course-filter">
        <el-input
          v-model="keyword"
          clearable
          :prefix-icon="Search"
          placeholder="搜索年级或学科课程"
          style="width: 320px"
          @input="onKeywordChange"
        />
        <span>已创建 {{ courses.length }} 门课程</span>
      </div>

      <div v-loading="courseLoading" class="course-grid">
        <article
          v-for="course in visibleCourses"
          :key="course.courseId"
          class="course-card"
          @click="enterCourse(course)"
        >
          <div class="course-card-top">
            <span
              class="subject-badge large"
              :class="{
                physics: course.subject === 'PHYSICS',
                english: course.subject === 'ENGLISH',
                chinese: course.subject === 'CHINESE',
              }"
            >
              {{ course.subjectName.slice(0, 1) }}
            </span>
            <el-tag effect="plain" size="small">{{ course.gradeName }}</el-tag>
          </div>
          <h2>{{ course.subjectName }}</h2>
          <p>进入后管理本课程的资料开放、作业小题分与提分宝批次。</p>
          <div class="course-card-data">
            <span><b>{{ course.materialCount }}</b> 资料文件</span>
            <i></i>
            <span><b>{{ course.treasureBatchCount }}</b> 提分宝批次</span>
          </div>
          <el-button type="primary" plain>进入管理</el-button>
        </article>
      </div>
    </template>

    <template v-else>
      <div class="course-context">
        <button type="button" class="back-button" @click="backToCourses">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <div
          class="subject-badge large"
          :class="{
            physics: selectedCourse.subject === 'PHYSICS',
            english: selectedCourse.subject === 'ENGLISH',
            chinese: selectedCourse.subject === 'CHINESE',
          }"
        >
          {{ selectedCourse.subjectName.slice(0, 1) }}
        </div>
        <div>
          <h1>{{ courseTitle }}</h1>
          <p>管理课程资料及个性化学习文件。</p>
        </div>
        <el-tag v-if="isPhysics" type="success" effect="plain">支持物理作业小题分</el-tag>
      </div>

      <div class="content-card material-workbench">
        <el-tabs v-model="activeTab">
          <el-tab-pane name="materials">
            <template #label>
              <span class="tab-label"><el-icon><Files /></el-icon>资料管理</span>
            </template>
            <div class="tab-toolbar">
              <div>
                <strong>讲义与作业</strong>
                <span>共 {{ materials.length }} 个文件</span>
              </div>
              <div class="material-toolbar-right">
                <el-select
                  v-model="materialTypeFilter"
                  clearable
                  placeholder="全部类型"
                  style="width: 120px"
                  @change="loadMaterials"
                >
                  <el-option label="讲义" value="HANDOUT" />
                  <el-option label="作业" value="HOMEWORK" />
                </el-select>
                <el-button type="primary" :icon="Plus" @click="openMaterialCreate">新增资料</el-button>
              </div>
            </div>
            <el-table v-loading="materialLoading" :data="materials" border>
              <el-table-column label="资料文件" min-width="300">
                <template #default="{ row }">
                  <div class="file-cell">
                    <span class="file-icon">{{ upperExtension(row.fileName) }}</span>
                    <div>
                      <strong>{{ row.fileName }}</strong>
                      <span>{{ row.fileSizeText }}</span>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="类型" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.materialType === 'HOMEWORK' ? 'warning' : 'primary'" size="small">
                    {{ row.materialType === "HOMEWORK" ? "作业" : "讲义" }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="开放时间" width="165">
                <template #default="{ row }">{{ toDisplayDateTime(row.openTime) }}</template>
              </el-table-column>
              <el-table-column label="开放状态" width="95" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.openStatus === 'OPEN' ? 'success' : 'info'" size="small">
                    {{ row.openStatus === "OPEN" ? "已开放" : "待开放" }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column v-if="isPhysics" label="小题分 / 提交情况" min-width="185">
                <template #default="{ row }">
                  <template v-if="row.materialType === 'HOMEWORK'">
                    <strong>{{ row.questionCount || 0 }} 题 / {{ row.totalScore || 0 }} 分</strong>
                    <div class="deadline-text">截止：{{ toDisplayDateTime(row.submitDeadline) || "未设置" }}</div>
                    <div class="submission-progress">
                      <el-progress
                        :percentage="row.authorizedStudentCount ? Math.round((row.submittedCount / row.authorizedStudentCount) * 100) : 0"
                        :stroke-width="5"
                        :show-text="false"
                      />
                      <span>{{ row.submittedCount }}/{{ row.authorizedStudentCount }} 已提交</span>
                    </div>
                  </template>
                  <span v-else class="subtle-text">—</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="210" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" :icon="Edit" @click="openMaterialEdit(row)">编辑</el-button>
                  <el-button
                    v-if="isPhysics && row.materialType === 'HOMEWORK'"
                    link
                    type="primary"
                    :icon="Download"
                    @click="exportSubmissions(row)"
                  >
                    导出小题分
                  </el-button>
                  <el-button link type="danger" :icon="Delete" @click="removeMaterial(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane name="treasure">
            <template #label>
              <span class="tab-label"><el-icon><Files /></el-icon>提分宝管理</span>
            </template>
            <div class="tab-toolbar">
              <div>
                <strong>提分宝上传批次</strong>
                <span>按上传时间倒序排列</span>
              </div>
              <el-button type="primary" :icon="Plus" @click="openTreasureCreate">新增提分宝</el-button>
            </div>
            <el-table v-loading="treasureLoading" :data="treasureBatches" border>
              <el-table-column label="关联作业" min-width="300">
                <template #default="{ row }">
                  <strong>{{ row.homeworkTitle }}</strong>
                </template>
              </el-table-column>
              <el-table-column prop="fileName" label="压缩包" min-width="240" />
              <el-table-column prop="parsedFileCount" label="解析文件数" width="110" align="center" />
              <el-table-column label="发布状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.publishStatus === 'PUBLISHED' ? 'success' : 'info'" size="small">
                    {{ row.publishStatus === "PUBLISHED" ? "已发布" : row.publishStatus === "REVOKED" ? "已撤销" : row.publishStatus }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="上传时间" width="165">
                <template #default="{ row }">{{ toDisplayDateTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="90" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button link type="danger" :icon="Delete" @click="removeBatch(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>

    <el-dialog
      v-model="materialVisible"
      :title="materialMode === 'create' ? '新增课程资料' : '编辑课程资料'"
      width="820px"
      @closed="resetMaterialForm"
    >
      <el-form ref="materialFormRef" label-width="86px">
        <el-form-item label="资料类型">
          <el-radio-group v-model="materialForm.materialType">
            <el-radio-button value="HANDOUT">讲义</el-radio-button>
            <el-radio-button value="HOMEWORK">作业</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="materialMode === 'create'" label="上传文件">
          <el-upload
            drag
            multiple
            :auto-upload="false"
            :on-change="handleMaterialFiles"
            :on-remove="handleMaterialFiles"
            style="width: 100%"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖放多个资料文件到这里，或 <em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 pdf/doc/docx/xls/xlsx/ppt/pptx，每个文件需单独设置开放时间。</div>
            </template>
          </el-upload>
        </el-form-item>

        <div v-if="materialForm.files.length" class="upload-file-list">
          <article v-for="(file, fileIndex) in materialForm.files" :key="file.uid" class="upload-file-item">
            <div class="upload-file-head">
              <span class="file-order">{{ fileIndex + 1 }}</span>
              <div>
                <strong>{{ file.name }}</strong>
                <span>{{ file.sizeText }}</span>
              </div>
              <el-button
                v-if="materialMode === 'create'"
                link
                type="danger"
                :icon="Delete"
                @click="removeUploadItem(file.uid)"
              >
                移除
              </el-button>
            </div>
            <div class="file-setting-row">
              <label>开放时间</label>
              <el-date-picker
                v-model="file.openAt"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm"
                format="YYYY-MM-DD HH:mm"
                placeholder="选择开放时间"
                style="width: 220px"
              />
            </div>
            <div v-if="isPhysics && materialForm.materialType === 'HOMEWORK'" class="score-setting">
              <div class="score-heading">
                <div>
                  <strong>作业小题分设置</strong>
                  <span>为物理作业配置题目数量和每道题分值</span>
                </div>
                <div class="question-count">
                  <label>题目数量</label>
                  <el-input-number
                    v-model="file.questionCount"
                    :min="1"
                    :max="30"
                    size="small"
                    @change="updateQuestionCount(file)"
                  />
                </div>
              </div>
              <div class="score-grid">
                <div v-for="(_, scoreIndex) in file.scores" :key="scoreIndex" class="score-item">
                  <span>第 {{ scoreIndex + 1 }} 题</span>
                  <el-input-number v-model="file.scores[scoreIndex]" :min="1" :max="20" size="small" />
                  <i>分</i>
                </div>
              </div>
              <div class="deadline-setting">
                <label>提交截止时间</label>
                <el-date-picker
                  v-model="file.submitDeadline"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm"
                  format="YYYY-MM-DD HH:mm"
                  placeholder="选择作业提交截止时间"
                  style="width: 230px"
                />
                <span>必须晚于资料开放时间</span>
              </div>
              <div class="score-total">
                总分：<b>{{ file.scores.reduce((sum, score) => sum + Number(score), 0) }}</b> 分
              </div>
            </div>
          </article>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="materialVisible = false">取消</el-button>
        <el-button type="primary" :loading="materialSubmitting" @click="saveMaterials">
          {{ materialMode === "create" ? "确认上传" : "保存修改" }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="treasureVisible" title="上传提分宝" width="660px">
      <el-form label-width="92px">
        <el-form-item label="关联作业" required>
          <el-select
            v-model="treasureForm.homeworkMaterialId"
            placeholder="请选择已对学生显示的作业"
            style="width: 100%"
          >
            <el-option
              v-for="homework in treasureHomeworkList"
              :key="homework.homeworkMaterialId"
              :label="homework.treasureUploaded ? `${homework.homeworkTitle}（已上传提分宝）` : homework.homeworkTitle"
              :value="homework.homeworkMaterialId"
              :disabled="homework.treasureUploaded"
            >
              <div class="homework-option">
                <span>{{ homework.homeworkTitle }}</span>
                <el-tag v-if="homework.treasureUploaded" type="info" size="small">已上传</el-tag>
                <small v-else>显示时间：{{ toDisplayDateTime(homework.displayTime) }}</small>
              </div>
            </el-option>
          </el-select>
          <div class="homework-select-tip">
            仅展示当前课程下已上传且已经对学生显示的作业；已上传过提分宝的作业不可重复选择。
          </div>
        </el-form-item>
        <el-form-item label="压缩包" required>
          <el-upload
            drag
            accept=".zip"
            :auto-upload="false"
            :limit="1"
            :on-change="handleTreasureFile"
            :on-remove="handleTreasureRemove"
            style="width: 100%"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖放 ZIP 压缩包到这里，或 <em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">选择后点击“解析压缩包”，后端会校验目录结构、学生匹配与路径安全。</div>
            </template>
          </el-upload>
        </el-form-item>
        <div v-if="parseStatus !== 'idle'" class="parse-result" :class="parseStatus">
          <div class="parse-result-head">
            <strong>
              {{
                parseStatus === "parsing"
                  ? "正在解析压缩包…"
                  : parseStatus === "success"
                    ? "解析成功，可以上传"
                    : "解析失败"
              }}
            </strong>
            <el-tag
              v-if="parseStatus !== 'parsing'"
              :type="parseStatus === 'success' ? 'success' : 'danger'"
            >
              {{ parseStatus === "success" ? "校验通过" : "不可上传" }}
            </el-tag>
          </div>
          <template v-if="parseStatus !== 'parsing' && parseResult">
            <div class="parse-metrics">
              <span>解析文件 <b>{{ parseResult.totalCount }}</b></span>
              <span>成功 <b class="success-text">{{ parseResult.successCount }}</b></span>
              <span>失败 <b class="danger-text">{{ parseResult.failedCount }}</b></span>
            </div>
            <p>{{ parseResult.message }}</p>
            <el-table
              v-if="parseResult.errors && parseResult.errors.length"
              :data="parseResult.errors"
              border
              max-height="180"
              size="small"
              style="margin-top: 10px"
            >
              <el-table-column prop="path" label="文件路径" min-width="180" show-overflow-tooltip />
              <el-table-column prop="studentNo" label="学号" width="120" />
              <el-table-column prop="message" label="错误原因" min-width="160" />
            </el-table>
          </template>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="treasureVisible = false">取消</el-button>
        <el-button :loading="parseStatus === 'parsing'" @click="handleParse">解析压缩包</el-button>
        <el-button
          type="primary"
          :loading="treasureSubmitting"
          :disabled="parseStatus !== 'success' || !treasureForm.homeworkMaterialId"
          @click="saveTreasure"
        >
          确认上传
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.course-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
  padding: 14px 16px;
}

.course-filter > span {
  color: #718096;
  font-size: 12px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(260px, 1fr));
  gap: 14px;
}

.course-card {
  min-height: 245px;
  padding: 18px;
  border: 1px solid #e1e6ee;
  border-radius: 7px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.course-card:hover {
  border-color: #9cc8ff;
  box-shadow: 0 10px 26px rgb(31 42 68 / 8%);
  transform: translateY(-2px);
}

.course-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.subject-badge.large {
  width: 42px;
  height: 42px;
  font-size: 16px;
}

.course-card h2 {
  margin: 16px 0 7px;
  font-size: 18px;
}

.course-card p {
  min-height: 40px;
  margin: 0;
  color: #718096;
  font-size: 12px;
  line-height: 1.65;
}

.course-card-data {
  display: flex;
  align-items: center;
  gap: 13px;
  margin: 18px 0;
  color: #718096;
  font-size: 12px;
}

.course-card-data b {
  color: #26344c;
  font-size: 15px;
}

.course-card-data i {
  width: 1px;
  height: 15px;
  background: #dce2ea;
}

.course-context {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.course-context h1 {
  margin: 0;
  font-size: 20px;
}

.course-context p {
  margin: 4px 0 0;
  color: #718096;
  font-size: 12px;
}

.course-context .el-tag {
  margin-left: auto;
}

.back-button {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid #d9dee7;
  border-radius: 5px;
  color: #475467;
  background: #fff;
  cursor: pointer;
}

.back-button:hover {
  color: #1677ff;
  border-color: #8dc0ff;
}

.material-workbench {
  min-height: 540px;
  padding: 0 16px 16px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 14px;
}

.tab-toolbar strong {
  font-size: 14px;
}

.tab-toolbar span {
  margin-left: 8px;
  color: #98a2b3;
  font-size: 12px;
}

.material-toolbar-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.file-cell,
.upload-file-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.file-icon {
  display: grid;
  width: 38px;
  min-width: 38px;
  height: 40px;
  place-items: center;
  border-radius: 4px;
  color: #1677ff;
  background: #eaf3ff;
  font-size: 9px;
  font-weight: 800;
}

.file-cell > div,
.upload-file-head > div {
  display: grid;
  min-width: 0;
}

.file-cell strong,
.upload-file-head strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-cell span:not(.file-icon),
.upload-file-head span {
  margin-top: 3px;
  color: #98a2b3;
  font-size: 11px;
}

.submission-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 5px;
}

.submission-progress .el-progress {
  width: 80px;
}

.submission-progress span {
  color: #718096;
  font-size: 11px;
}

.deadline-text {
  margin-top: 4px;
  color: #f97316;
  font-size: 11px;
}

.upload-file-list {
  display: grid;
  gap: 12px;
  margin-left: 86px;
}

.upload-file-item {
  padding: 14px;
  border: 1px solid #dfe5ed;
  border-radius: 6px;
  background: #fbfcfe;
}

.upload-file-head .el-button {
  margin-left: auto;
}

.file-order {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 50%;
  color: #fff !important;
  background: #60748f;
  font-size: 11px !important;
}

.file-setting-row {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-top: 13px;
  padding-top: 13px;
  border-top: 1px dashed #dce2ea;
}

.file-setting-row label,
.question-count label {
  color: #536176;
  font-size: 12px;
}

.score-setting {
  margin-top: 13px;
  padding: 13px;
  border: 1px solid #bfe4da;
  border-radius: 5px;
  background: #f3fbf8;
}

.score-heading,
.question-count {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.score-heading > div:first-child {
  display: grid;
}

.score-heading strong {
  color: #166453;
  font-size: 13px;
}

.score-heading span {
  margin-top: 3px;
  color: #67877e;
  font-size: 11px;
}

.question-count {
  gap: 10px;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 13px;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 7px 9px;
  border: 1px solid #d7ebe5;
  border-radius: 4px;
  background: #fff;
}

.score-item span,
.score-item i {
  color: #536f68;
  font-size: 11px;
  font-style: normal;
  white-space: nowrap;
}

.score-item .el-input-number {
  width: 86px;
}

.score-total {
  margin-top: 10px;
  color: #536f68;
  text-align: right;
  font-size: 12px;
}

.deadline-setting {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #cde1dc;
}

.deadline-setting label {
  color: #166453;
  font-size: 12px;
  font-weight: 650;
}

.deadline-setting span {
  color: #67877e;
  font-size: 11px;
}

.score-total b {
  color: #13866d;
}

.parse-result {
  margin-left: 92px;
  padding: 14px;
  border: 1px solid #dfe5ed;
  border-radius: 6px;
  background: #f8fafc;
}

.homework-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.homework-option small {
  color: #98a2b3;
  font-size: 11px;
}

.homework-select-tip {
  margin-top: 7px;
  color: #98a2b3;
  font-size: 11px;
  line-height: 1.5;
}

.parse-result.success {
  border-color: #bfe4cc;
  background: #f2fbf5;
}

.parse-result.failed {
  border-color: #f2c7c7;
  background: #fff6f6;
}

.parse-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.parse-metrics {
  display: flex;
  gap: 28px;
  margin-top: 13px;
  color: #718096;
  font-size: 12px;
}

.parse-metrics b {
  color: #26344c;
}

.parse-result p {
  margin: 9px 0 0;
  color: #718096;
  font-size: 12px;
}

.success-text {
  color: #16a34a !important;
}

.danger-text {
  color: #dc2626 !important;
}
</style>
