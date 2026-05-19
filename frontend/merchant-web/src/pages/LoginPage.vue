<template>
  <section class="login-panel">
    <h1>商家登录</h1>
    <p class="muted">测试账号：13800000001 / 123456</p>

    <form @submit.prevent="submitLogin" class="form-grid">
      <label>
        手机号
        <input v-model.trim="mobile" type="text" placeholder="13800000001" />
      </label>

      <label>
        验证码
        <input v-model.trim="verifyCode" type="text" placeholder="123456" />
      </label>

      <button type="submit" :disabled="loading">{{ loading ? "登录中..." : "登录" }}</button>
    </form>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
  </section>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";

interface LoginData {
  access_token: string;
  tenant_id: number;
}

interface LoginApiResponse {
  code: number;
  message: string;
  data: LoginData;
}

const mobile = ref("13800000001");
const verifyCode = ref("123456");
const loading = ref(false);
const errorMessage = ref("");

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

function resolveRedirect(): string {
  const redirect = route.query.redirect;
  if (typeof redirect === "string" && redirect.startsWith("/")) {
    return redirect;
  }
  return "/dashboard";
}

async function submitLogin() {
  errorMessage.value = "";
  loading.value = true;
  try {
    const response = await fetch("/api/v1/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Request-Id": crypto.randomUUID(),
      },
      body: JSON.stringify({
        mobile: mobile.value,
        verify_code: verifyCode.value,
      }),
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const payload = (await response.json()) as LoginApiResponse;
    if (payload.code !== 0) {
      throw new Error(payload.message || "登录失败");
    }

    auth.setAccessToken(payload.data.access_token);
    auth.setTenantId(String(payload.data.tenant_id));
    await auth.loadProfile();
    await router.replace(resolveRedirect());
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "登录失败";
    auth.clearAuth();
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-panel {
  max-width: 420px;
  margin: 40px auto;
  background: #ffffff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(18, 30, 61, 0.08);
}

.form-grid {
  display: grid;
  gap: 12px;
}

label {
  display: grid;
  gap: 6px;
  font-size: 14px;
}

input {
  border: 1px solid #d7deea;
  border-radius: 6px;
  padding: 8px 10px;
}

button {
  margin-top: 6px;
  border: none;
  border-radius: 6px;
  padding: 10px 12px;
  background: #2058d6;
  color: white;
  cursor: pointer;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.muted {
  color: #5a677f;
  font-size: 13px;
}

.error {
  color: #c83a28;
  margin-top: 8px;
}
</style>
