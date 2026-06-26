import { getData, postData, putData, downloadBlob, uploadFiles } from "@/utils/request";

// GET /study/student/list -> { total, rows }
export function listStudents(params) {
  return getData("/study/student/list", params).then((data) => ({
    total: 0,
    rows: [],
    ...data,
  }));
}

// GET /study/student/{studentId}
export function getStudent(studentId) {
  return getData(`/study/student/${studentId}`);
}

// POST /study/student
export function createStudent(data) {
  return postData("/study/student", data);
}

// PUT /study/student
export function updateStudent(data) {
  return putData("/study/student", data);
}

// POST /study/student/import-template -> xlsx blob
export function downloadImportTemplate() {
  return downloadBlob("/study/student/import-template", null, { method: "POST" });
}

// POST /study/student/import/validate (multipart file)
export function validateImport(file) {
  return uploadFiles("/study/student/import/validate", { file });
}

// POST /study/student/import/confirm { importToken, updateSupport }
export function confirmImport(data) {
  return postData("/study/student/import/confirm", data);
}

// POST /study/student/export -> xlsx blob
export function exportStudents(params) {
  return downloadBlob("/study/student/export", params, { method: "POST" });
}
