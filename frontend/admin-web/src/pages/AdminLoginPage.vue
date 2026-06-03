<template>
  <section class="login-panel">
    <h1>管理端登录</h1>

    <form @submit.prevent="submitLogin" class="form-grid">
      <label>
        手机号
        <input v-model.trim="mobile" type="text" placeholder="请输入手机号" class="input" />
      </label>
      <label>
        验证码
        <div class="verify-code-row">
          <input v-model.trim="verifyCode" type="text" placeholder="请输入验证码" class="input" />
          <button type="button" :disabled="smsCooldown > 0 || !mobileValid" class="sms-btn" @click="sendSmsCode">
            {{ smsCooldown > 0 ? `${smsCooldown}s` : "获取验证码" }}
          </button>
        </div>
      </label>
      <button type="submit" :disabled="loading" class="btn-primary">{{ loading ? "登录中..." : "登录" }}</button>
    </form>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
  </section>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAdminAuthStore } from "../stores/adminAuth";

const mobile = ref("");
const verifyCode = ref("");
const loading = ref(false);
const errorMessage = ref("");
const smsCooldown = ref(0);

const mobileValid = computed(() => /^1[3-9]\d{9}$/.test(mobile.value));

const route = useRoute();
const router = useRouter();
const auth = useAdminAuthStore();

function resolveRedirect(): string {
  const redirect = route.query.redirect;
  if (typeof redirect === "string" && redirect.startsWith("/")) {
    return redirect;
  }
  return "/admin/dashboard";
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
  loading.value = true;
  errorMessage.value = "";
  try {
    await auth.login(mobile.value, verifyCode.value);
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
  margin: 80px auto;
  background: hsl(var(--card));
  border-radius: var(--radius);
  padding: 32px;
  box-shadow: var(--shadow-md);
  border: 1px solid hsl(var(--border));
}

h1 {
  color: hsl(var(--card-foreground));
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.muted {
  color: hsl(var(--muted-foreground));
  font-size: 13px;
  margin: 0;
}

.verify-code-row {
  display: flex;
  gap: 8px;
}

.verify-code-row .input {
  flex: 1;
}

.sms-btn {
  white-space: nowrap;
  padding: 10px 12px;
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  background: hsl(var(--card));
  color: hsl(var(--foreground));
  font-size: 13px;
  cursor: pointer;
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

.form-grid {
  display: grid;
  gap: 16px;
  margin-top: 24px;
}

label {
  display: grid;
  gap: 8px;
  font-size: 14px;
  color: hsl(var(--card-foreground));
  font-weight: 500;
}

.input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  background: hsl(var(--background));
  color: hsl(var(--foreground));
  font-size: 14px;
  transition: border-color 0.2s ease;
  box-sizing: border-box;
}

.input:hover {
  border-color: hsl(var(--muted-foreground));
}

.input:focus {
  outline: none;
  border-color: hsl(var(--primary));
  box-shadow: 0 0 0 2px hsl(var(--primary) / 0.2);
}

.btn-primary {
  margin-top: 8px;
  padding: 12px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  border: none;
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: hsl(var(--destructive));
  background: hsl(var(--destructive) / 0.1);
  padding: 12px;
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  text-align: center;
  margin-top: 16px;
  border: 1px solid hsl(var(--destructive) / 0.3);
}
</style>