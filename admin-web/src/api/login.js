import { getData, postData } from "@/utils/request";

// POST /login { username, password } -> { token, expiresIn }
export function login(data) {
  return postData("/login", data);
}

// GET /getInfo -> { user, roles, permissions }
export function getUserInfo() {
  return getData("/getInfo");
}
