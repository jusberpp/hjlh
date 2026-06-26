#!/usr/bin/env python3
import argparse
import base64
import json
import os
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zipfile
from pathlib import Path


PNG_1X1 = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
)
PDF_BYTES = b"%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\n%%EOF\n"


class SmokeError(RuntimeError):
    pass


def request(base, method, path, token=None, student_token=None, data=None, files=None, query=None, expect_json=True):
    if query:
        path += "?" + urllib.parse.urlencode(query)
    url = base.rstrip("/") + path
    headers = {}
    body = None
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if student_token:
        headers["token"] = student_token
    if files:
        body, content_type = multipart(data or {}, files)
        headers["Content-Type"] = content_type
    elif data is not None:
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            payload = resp.read()
            status = resp.status
            content_type = resp.headers.get("content-type", "")
    except urllib.error.HTTPError as err:
        payload = err.read()
        status = err.code
        content_type = err.headers.get("content-type", "")
    if expect_json and "application/json" in content_type:
        try:
            parsed = json.loads(payload.decode("utf-8"))
        except json.JSONDecodeError as exc:
            raise SmokeError(f"{method} {path} returned invalid JSON: {exc}") from exc
        return status, parsed, payload
    if expect_json:
        try:
            parsed = json.loads(payload.decode("utf-8"))
            return status, parsed, payload
        except Exception:
            pass
    return status, None, payload


def multipart(fields, files):
    boundary = "----codex-smoke-" + uuid.uuid4().hex
    chunks = []
    for name, value in fields.items():
        chunks.extend([
            f"--{boundary}\r\n".encode(),
            f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode(),
            str(value).encode("utf-8"),
            b"\r\n",
        ])
    for name, file_info in files.items():
        filename, content_type, content = file_info
        chunks.extend([
            f"--{boundary}\r\n".encode(),
            f'Content-Disposition: form-data; name="{name}"; filename="{filename}"\r\n'.encode(),
            f"Content-Type: {content_type}\r\n\r\n".encode(),
            content,
            b"\r\n",
        ])
    chunks.append(f"--{boundary}--\r\n".encode())
    return b"".join(chunks), f"multipart/form-data; boundary={boundary}"


def must_ok(step, status, payload):
    code = payload.get("code") if isinstance(payload, dict) else None
    if status >= 400 or code not in (None, 200):
        raise SmokeError(f"{step} failed: http={status}, body={payload}")
    print(f"PASS {step}")
    return payload


def must_fail_business(step, status, payload):
    code = payload.get("code") if isinstance(payload, dict) else None
    if status >= 500 or code in (None, 200):
        raise SmokeError(f"{step} expected business failure, got http={status}, body={payload}")
    print(f"PASS {step} -> expected code {code}: {payload.get('msg')}")
    return payload


def login(base):
    status, payload, _ = request(base, "POST", "/login", data={"username": "admin", "password": "Admin123!"})
    must_ok("admin login", status, payload)
    return payload["data"]["token"]


def create_course(base, token):
    unique = int(time.time())
    payload = {
        "grade": "SENIOR_THREE",
        "subject": "PHYSICS",
        "lecturerName": "联调讲师",
        "sourceValue": f"smoke-physics-{unique}",
        "classTimes": [
            {"startTime": "2026-07-10T09:00:00", "endTime": "2026-07-10T11:00:00"},
            {"startTime": "2026-07-12T09:00:00", "endTime": "2026-07-12T11:00:00"},
        ],
    }
    status, body, _ = request(base, "POST", "/course/course", token=token, data=payload)
    if body and (body.get("code") == 42207 or "课程已存在" in str(body.get("msg", ""))):
        status, body, _ = request(base, "GET", "/course/course/list", token=token, query={
            "grade": "SENIOR_THREE",
            "subject": "PHYSICS",
            "pageNum": 1,
            "pageSize": 1,
        })
        must_ok("course list fallback", status, body)
        return body["rows"][0]["courseId"]
    must_ok("course create with multiple class times", status, body)
    return body["data"]["courseId"]


def make_zip(student_no):
    with tempfile.NamedTemporaryFile(suffix=".zip", delete=False) as tmp:
        zip_path = Path(tmp.name)
    try:
        with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as archive:
            archive.writestr(f"{student_no}.pdf", PDF_BYTES)
        return zip_path.read_bytes()
    finally:
        zip_path.unlink(missing_ok=True)


def run(base, proxy_base=None):
    status, payload, _ = request(base, "GET", "/actuator/health")
    if status != 200 or payload.get("status") != "UP":
        raise SmokeError(f"health failed: http={status}, body={payload}")
    print("PASS health")

    token = login(base)
    status, payload, _ = request(base, "GET", "/getInfo", token=token)
    must_ok("admin getInfo", status, payload)

    status, payload, _ = request(base, "GET", "/course/course/options", token=token)
    must_ok("course options", status, payload)

    status, payload, _ = request(base, "POST", "/course/course/lecturer-avatar", token=token, files={
        "file": ("avatar.png", "image/png", PNG_1X1)
    })
    must_ok("lecturer avatar upload", status, payload)
    avatar_url = payload["data"]["lecturerAvatarUrl"]

    course_id = create_course(base, token)
    status, payload, _ = request(base, "GET", f"/course/course/{course_id}", token=token)
    must_ok("course detail", status, payload)

    update_payload = {
        "courseId": course_id,
        "grade": "SENIOR_THREE",
        "subject": "PHYSICS",
        "lecturerName": "联调讲师更新",
        "lecturerAvatarUrl": avatar_url,
        "sourceValue": f"smoke-physics-updated-{int(time.time())}",
        "classTimes": [
            {"startTime": "2026-07-10T09:00:00", "endTime": "2026-07-10T11:00:00"},
            {"startTime": "2026-07-13T09:00:00", "endTime": "2026-07-13T11:00:00"},
        ],
        "status": "ENABLED",
    }
    status, payload, _ = request(base, "PUT", "/course/course", token=token, data=update_payload)
    must_ok("course update", status, payload)
    for state in ("DISABLED", "ENABLED"):
        status, payload, _ = request(base, "PUT", "/course/course/status", token=token, data={
            "courseId": course_id,
            "status": state,
        })
        must_ok(f"course status {state}", status, payload)

    run_id = int(time.time())
    phone_suffix = f"{run_id % 100000000:08d}"
    student_no = "SMK" + str(run_id)
    student_phone = "138" + phone_suffix
    duplicate_test_phone = "137" + phone_suffix
    bind_phone = "139" + phone_suffix
    student_payload = {
        "school": "联调测试中学",
        "grade": "SENIOR_THREE",
        "className": "1班",
        "studentName": "联调学生",
        "studentNo": student_no,
        "courseIds": [course_id],
        "primaryCourseId": course_id,
        "authorizedPhone": student_phone,
    }
    status, payload, _ = request(base, "POST", "/study/student", token=token, data=student_payload)
    must_ok("student create with primary course", status, payload)
    student_id = payload["data"]["studentId"]

    updated_student_payload = dict(student_payload)
    updated_student_payload["studentId"] = student_id
    updated_student_payload["className"] = "2班"
    status, payload, _ = request(base, "PUT", "/study/student", token=token, data=updated_student_payload)
    must_ok("student update", status, payload)
    status, payload, _ = request(base, "GET", f"/study/student/{student_id}", token=token)
    must_ok("student detail", status, payload)

    duplicate_no = dict(student_payload)
    duplicate_no["studentName"] = "重复学号"
    duplicate_no["authorizedPhone"] = duplicate_test_phone
    status, payload, _ = request(base, "POST", "/study/student", token=token, data=duplicate_no)
    must_fail_business("studentNo uniqueness", status, payload)

    duplicate_phone = dict(student_payload)
    duplicate_phone["studentNo"] = student_no + "P"
    duplicate_phone["studentName"] = "重复手机号"
    status, payload, _ = request(base, "POST", "/study/student", token=token, data=duplicate_phone)
    must_fail_business("phone uniqueness", status, payload)

    status, payload, template_bytes = request(
        base, "POST", "/study/student/import-template", token=token, expect_json=False
    )
    if status != 200 or len(template_bytes) < 100:
        raise SmokeError(f"student import template failed: http={status}, bytes={len(template_bytes)}")
    print("PASS student import template")
    status, payload, _ = request(base, "POST", "/study/student/import/validate", token=token, files={
        "file": ("template.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", template_bytes)
    })
    must_ok("student import validate", status, payload)

    status, payload, _ = request(base, "POST", "/course/material/upload", token=token, files={
        "files": ("homework.pdf", "application/pdf", PDF_BYTES)
    })
    must_ok("material upload", status, payload)
    upload_token = payload["data"][0]["uploadToken"]
    status, payload, _ = request(base, "POST", "/course/material/batch", token=token, data={
        "courseId": course_id,
        "materialType": "HOMEWORK",
        "files": [{
            "uploadToken": upload_token,
            "openTime": "2026-06-01T08:00:00",
            "submitDeadline": "2026-07-01T22:00:00",
            "questionCount": 2,
            "questionScores": [{"questionNo": 1, "score": 5}, {"questionNo": 2, "score": 5}],
        }],
    })
    must_ok("material batch with display/open time and physics scores", status, payload)
    homework_id = payload["data"]["materialIds"][0]
    status, payload, _ = request(base, "GET", "/course/material/list", token=token, query={
        "courseId": course_id,
        "materialType": "HOMEWORK",
    })
    must_ok("material list", status, payload)

    status, payload, _ = request(base, "GET", "/course/treasure/homework-options", token=token, query={
        "courseId": course_id,
    })
    must_ok("treasure homework options", status, payload)
    status, payload, _ = request(base, "POST", "/course/treasure/parse", token=token, data={
        "courseId": course_id,
        "homeworkMaterialId": homework_id,
    }, files={
        "file": ("treasure.zip", "application/zip", make_zip(student_no))
    })
    must_ok("treasure zip parse", status, payload)
    if not payload["data"].get("canSubmit"):
        raise SmokeError(f"treasure parse cannot submit: {payload}")
    parse_token = payload["data"]["parseToken"]
    status, payload, _ = request(base, "POST", "/course/treasure", token=token, data={
        "courseId": course_id,
        "homeworkMaterialId": homework_id,
        "parseToken": parse_token,
    })
    must_ok("treasure confirm batch", status, payload)
    batch_id = payload["data"]["batchId"]
    status, payload, _ = request(base, "GET", "/course/treasure/list", token=token, query={
        "courseId": course_id,
    })
    must_ok("treasure list", status, payload)
    status, payload, _ = request(base, "DELETE", f"/course/treasure/{batch_id}", token=token)
    must_ok("treasure delete batch", status, payload)

    status, payload, _ = request(base, "POST", "/auth/verify-student", data={
        "school": "联调测试中学",
        "grade": "高三",
        "name": "联调学生",
        "studentId": student_no,
    })
    must_ok("old H5 verify-student", status, payload)
    h5_token = payload["data"]["token"]
    status, payload, _ = request(base, "POST", "/auth/bind-phone", student_token=h5_token, data={
        "id": str(student_id),
        "phone": bind_phone,
    })
    must_ok("old H5 bind-phone token header", status, payload)
    status, payload, _ = request(base, "GET", "/auth/bind-result", student_token=h5_token)
    must_ok("old H5 bind-result", status, payload)
    status, payload, _ = request(base, "GET", f"/auth/files/{homework_id}/download", student_token=h5_token, expect_json=False)
    if status != 200:
        raise SmokeError(f"old H5 authenticated material download failed: http={status}")
    print("PASS old H5 authenticated material download")
    status, payload, _ = request(base, "GET", f"/auth/files/{homework_id}/download", query={"bypassToken": "true"})
    must_fail_business("old H5 bypassToken rejected", status, payload)

    if proxy_base:
        proxy_token = login(proxy_base)
        status, payload, _ = request(proxy_base, "GET", "/getInfo", token=proxy_token)
        must_ok("admin-web vite proxy getInfo", status, payload)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default=os.environ.get("SMOKE_BASE", "http://localhost:8081"))
    parser.add_argument("--proxy-base", default=os.environ.get("SMOKE_PROXY_BASE"))
    args = parser.parse_args()
    try:
        run(args.base, args.proxy_base)
    except SmokeError as exc:
        print(f"FAIL {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
