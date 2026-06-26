// 通用格式化辅助。

export function formatFileSize(bytes) {
  if (bytes === undefined || bytes === null) return "";
  const size = Number(bytes);
  if (Number.isNaN(size)) return "";
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

export function fileExtension(fileName) {
  if (!fileName) return "";
  const index = fileName.lastIndexOf(".");
  return index < 0 ? "" : fileName.slice(index + 1).toLowerCase();
}

export function upperExtension(fileName) {
  return fileExtension(fileName).toUpperCase();
}
