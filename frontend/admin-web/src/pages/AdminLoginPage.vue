<template>
  <section class="login-panel">
    <h1>管理端登录</h1>
    <p class="muted">种子账号：13900000001 / 123456</p>

    <form @submit.prevent="submitLogin" class="form-grid">
      <label>
        手机号
        <input v-model.trim="mobile" type="text" placeholder="13900000001" />
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
import { useAdminAuthStore } from "../stores/adminAuth";

const mobile = ref("13900000001");
const verifyCode = ref("123456");
const loading = ref(false);
const errorMessage = ref("");

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
}

.muted {
  color: #5a677f;
}

.error {
  color: #c83a28;
}
</style>