<template>
  <div class="login-page">
    <section class="login-panel">
      <h1>商家登录</h1>

      <form @submit.prevent="submitLogin" class="form-grid">
        <label>
          手机号
          <input v-model.trim="mobile" type="text" placeholder="请输入手机号" />
        </label>

        <label>
          验证码
          <div class="verify-code-row">
            <input v-model.trim="verifyCode" type="text" placeholder="请输入验证码" />
            <button type="button" :disabled="smsCooldown > 0 || !mobileValid" class="sms-btn" @click="sendSmsCode">
              {{ smsCooldown > 0 ? `${smsCooldown}s` : "获取验证码" }}
            </button>
          </div>
        </label>

        <button type="submit" :disabled="loading" class="login-btn">{{ loading ? "登录中..." : "登录" }}</button>
      </form>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
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

const mobile = ref("");
const verifyCode = ref("");
const loading = ref(false);
const errorMessage = ref("");
const smsCooldown = ref(0);

const mobileValid = computed(() => /^1[3-9]\d{9}$/.test(mobile.value));

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

async function sendSmsCode() {
  if (!mobileValid.value || smsCooldown.value > 0) return;
  errorMessage.value = "";
  try {
    const response = await fetch(`/api/v1/sms/send-login-code?mobile=${encodeURIComponent(mobile.value)}`, {
      method: "POST",
      headers: { "X-Request-Id": crypto.randomUUID() },
    });
    const payload = await response.json();
    if (payload.code !== 0) {
      throw new Error(payload.message || "发送验证码失败");
    }
    smsCooldown.value = 60;
    const timer = setInterval(() => {
      smsCooldown.value--;
      if (smsCooldown.value <= 0) clearInterval(timer);
    }, 1000);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "发送验证码失败";
  }
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
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: hsl(var(--background));
  color: hsl(var(--foreground));
  padding: 16px;
}

@media (min-width: 768px) {
  .login-page {
    padding: 24px;
  }
}

.login-panel {
  width: 100%;
  max-width: 420px;
  background: hsl(var(--card));
  border-radius: 10px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  border: 1px solid hsl(var(--border));
}

@media (min-width: 768px) {
  .login-panel {
    padding: 32px;
  }
}

h1 {
  color: hsl(var(--foreground));
  margin-bottom: 8px;
}

.form-grid {
  display: grid;
  gap: 12px;
}

label {
  display: grid;
  gap: 6px;
  font-size: 14px;
  color: hsl(var(--foreground));
}

input {
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  padding: 8px 10px;
  background: hsl(var(--input));
  color: hsl(var(--foreground));
  font-size: 14px;
}

input::placeholder {
  color: hsl(var(--muted-foreground));
}

.verify-code-row {
  display: flex;
  gap: 8px;
}

.verify-code-row input {
  flex: 1;
}

.sms-btn {
  white-space: nowrap;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  padding: 8px 12px;
  background: hsl(var(--card));
  color: hsl(var(--foreground));
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s ease;
}

.sms-btn:hover:not(:disabled) {
  background: hsl(var(--primary) / 0.1);
  border-color: hsl(var(--primary));
}

.sms-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.login-btn {
  margin-top: 6px;
  border: none;
  border-radius: 6px;
  padding: 10px 12px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.login-btn:hover:not(:disabled) {
  background: hsl(var(--primary) / 0.9);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: hsl(var(--destructive));
  margin-top: 8px;
  font-size: 13px;
}
</style>
