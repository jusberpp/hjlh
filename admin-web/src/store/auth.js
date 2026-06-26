import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { login as loginApi, getUserInfo } from "@/api/login";
import { getToken, setToken, removeToken } from "@/utils/request";

export const useAuthStore = defineStore("auth", () => {
  const token = ref(getToken());
  const userId = ref(null);
  const username = ref("");
  const nickName = ref("");
  const orgId = ref(null);
  const permissions = ref([]);
  const loaded = ref(false);

  const isLoggedIn = computed(() => Boolean(token.value));
  const displayName = computed(() => nickName.value || username.value || "管理员");

  function hasPermission(permission) {
    const list = permissions.value;
    return list.includes("*") || list.includes(permission);
  }

  async function login(payload) {
    const data = await loginApi(payload);
    token.value = data.token;
    setToken(data.token);
    await fetchUserInfo();
    return data;
  }

  async function fetchUserInfo() {
    if (!token.value) return null;
    const info = await getUserInfo();
    const user = info.user || {};
    userId.value = user.userId;
    username.value = user.userName;
    nickName.value = user.nickName;
    orgId.value = user.orgId;
    permissions.value = info.permissions || [];
    loaded.value = true;
    return info;
  }

  function logout() {
    token.value = null;
    userId.value = null;
    username.value = "";
    nickName.value = "";
    orgId.value = null;
    permissions.value = [];
    loaded.value = false;
    removeToken();
  }

  return {
    token,
    userId,
    username,
    nickName,
    orgId,
    permissions,
    loaded,
    isLoggedIn,
    displayName,
    hasPermission,
    login,
    fetchUserInfo,
    logout,
  };
});
