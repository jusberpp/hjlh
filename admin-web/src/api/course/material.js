import { getData, postData, putData, deleteData, uploadFiles, downloadBlob } from "@/utils/request";

// GET /course/material/course-list -> 课程卡片列表
export function materialCourseList(params) {
  return getData("/course/material/course-list", params);
}

// GET /course/material/list -> 资料列表
export function listMaterials(params) {
  return getData("/course/material/list", params);
}

// POST /course/material/upload (多文件上传) -> [{ uploadToken, ... }]
export function uploadMaterials(files) {
  return uploadFiles("/course/material/upload", { files });
}

// POST /course/material/batch { courseId, materialType, files }
export function createMaterialBatch(data) {
  return postData("/course/material/batch", data);
}

// GET /course/material/{materialId}
export function getMaterial(materialId) {
  return getData(`/course/material/${materialId}`);
}

// PUT /course/material
export function updateMaterial(data) {
  return putData("/course/material", data);
}

// DELETE /course/material/{materialIds}
export function deleteMaterials(materialIds) {
  const ids = Array.isArray(materialIds) ? materialIds.join(",") : materialIds;
  return deleteData(`/course/material/${ids}`);
}

// POST /course/material/{materialId}/submission/export -> xlsx blob
export function exportSubmissionScores(materialId) {
  return downloadBlob(`/course/material/${materialId}/submission/export`, null, {
    method: "POST",
  });
}
