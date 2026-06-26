import { getData, postData, deleteData, uploadFiles } from "@/utils/request";

// GET /course/treasure/homework-options?courseId
export function homeworkOptions(courseId) {
  return getData("/course/treasure/homework-options", { courseId });
}

// GET /course/treasure/list?courseId -> { total, rows }
export function listTreasures(params) {
  return getData("/course/treasure/list", params).then((data) => ({
    total: 0,
    rows: [],
    ...data,
  }));
}

// POST /course/treasure/parse?courseId&homeworkMaterialId (multipart file)
export function parseTreasure(courseId, homeworkMaterialId, file) {
  return uploadFiles(
    "/course/treasure/parse",
    { file },
    { courseId, homeworkMaterialId },
  );
}

// POST /course/treasure { courseId, homeworkMaterialId, parseToken }
export function confirmTreasure(data) {
  return postData("/course/treasure", data);
}

// DELETE /course/treasure/{batchIds}
export function deleteTreasures(batchIds) {
  const ids = Array.isArray(batchIds) ? batchIds.join(",") : batchIds;
  return deleteData(`/course/treasure/${ids}`);
}
