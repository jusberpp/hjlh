import { getData, postData, putData, deleteData, uploadFiles } from "@/utils/request";

// GET /course/course/options -> { grades, subjects, courses }
export function courseOptions() {
  return getData("/course/course/options");
}

// GET /course/course/list -> { total, rows }
export function listCourses(params) {
  return getData("/course/course/list", params).then((data) => ({
    total: 0,
    rows: [],
    ...data,
  }));
}

// GET /course/course/{courseId}
export function getCourse(courseId) {
  return getData(`/course/course/${courseId}`);
}

// POST /course/course/lecturer-avatar (multipart file)
export function uploadLecturerAvatar(file) {
  return uploadFiles("/course/course/lecturer-avatar", { file });
}

// POST /course/course
export function createCourse(data) {
  return postData("/course/course", data);
}

// PUT /course/course
export function updateCourse(data) {
  return putData("/course/course", data);
}

// PUT /course/course/status { courseId, status }
export function updateCourseStatus(data) {
  return putData("/course/course/status", data);
}

// DELETE /course/course/{courseIds} (逗号分隔)
export function deleteCourses(courseIds) {
  const ids = Array.isArray(courseIds) ? courseIds.join(",") : courseIds;
  return deleteData(`/course/course/${ids}`);
}
