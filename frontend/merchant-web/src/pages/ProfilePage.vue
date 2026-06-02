<template>
  <section class="profile-container">
    <h1>个人中心</h1>

    <el-tabs v-model="activeTab" class="profile-tabs">
      <!-- 基本信息 -->
      <el-tab-pane label="基本信息" name="basic">
        <div class="profile-layout">
          <!-- 左侧：头像和基本信息 -->
          <div class="avatar-section">
            <div class="avatar-wrapper">
              <img :src="profile.avatarUrl || defaultAvatar" alt="Avatar" class="avatar" />
              <div class="avatar-actions">
                <el-button size="small" @click="handleUploadAvatar">更换头像</el-button>
                <input type="file" ref="fileInput" accept="image/*" style="display: none" @change="handleFileChange" />
              </div>
            </div>

            <div class="user-info">
              <h2>{{ profile.nickname || '未设置昵称' }}</h2>
              <p class="user-role">{{ getRoleName(profile.roleCode) }}</p>
            </div>

            <div class="binding-status">
              <div class="binding-item">
                <el-icon :class="{ 'bound': profile.wechatBound }">
                  <Check v-if="profile.wechatBound" />
                  <Close v-else />
                </el-icon>
                <span>微信：{{ profile.wechatBound ? '已绑定' : '未绑定' }}</span>
                <el-button v-if="!profile.wechatBound" link size="small" @click="handleBindWechat">去绑定</el-button>
                <el-button v-else link size="small" @click="handleUnbindWechat" type="danger">解绑</el-button>
              </div>
              <div class="binding-item">
                <el-icon :class="{ 'bound': !!profile.phone }">
                  <Check v-if="profile.phone" />
                  <Close v-else />
                </el-icon>
                <span>手机：{{ profile.phone || '未绑定' }}</span>
                <el-button link size="small" @click="showBindPhoneDialog = true">修改</el-button>
              </div>
            </div>
          </div>

          <!-- 右侧：编辑表单 -->
          <div class="form-section">
            <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="formData.nickname" placeholder="请输入昵称" maxlength="50" />
              </el-form-item>

              <el-form-item label="手机号">
                <el-input v-model="profile.mobile" disabled />
              </el-form-item>

              <el-form-item label="邮箱" prop="email">
                <el-input v-model="formData.email" placeholder="请输入邮箱" />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="handleSaveProfile" :loading="loading">保存</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-tab-pane>

      <!-- 账号安全 -->
      <el-tab-pane label="账号安全" name="security">
        <div class="security-section">
          <!-- 安全概览 -->
          <div class="security-overview">
            <h3>安全概览</h3>
            <div class="overview-grid">
              <div class="overview-item">
                <el-icon><Lock /></el-icon>
                <span>登录密码</span>
                <el-tag :type="security.hasPassword ? 'success' : 'warning'" size="small">
                  {{ security.hasPassword ? '已设置' : '未设置' }}
                </el-tag>
              </div>
              <div class="overview-item">
                <el-icon><Phone /></el-icon>
                <span>手机绑定</span>
                <el-tag :type="security.hasPhone ? 'success' : 'warning'" size="small">
                  {{ security.hasPhone ? '已绑定' : '未绑定' }}
                </el-tag>
              </div>
              <div class="overview-item">
                <el-icon><Message /></el-icon>
                <span>微信绑定</span>
                <el-tag :type="security.hasWechat ? 'success' : 'warning'" size="small">
                  {{ security.hasWechat ? '已绑定' : '未绑定' }}
                </el-tag>
              </div>
              <div class="overview-item">
                <el-icon><Message /></el-icon>
                <span>邮箱绑定</span>
                <el-tag :type="security.hasEmail ? 'success' : 'warning'" size="small">
                  {{ security.hasEmail ? '已绑定' : '未绑定' }}
                </el-tag>
              </div>
            </div>
            <div class="login-info" v-if="security.lastLoginTime">
              <p>最近登录：{{ security.lastLoginTime }} ({{ security.lastLoginIp }})</p>
              <p>登录设备数：{{ security.loginDeviceCount }}</p>
            </div>
          </div>

          <!-- 修改密码 -->
          <div class="password-section">
            <h3>修改密码</h3>
            <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="120px" style="max-width: 500px">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码（6-50位）" />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleChangePassword" :loading="changingPassword">修改密码</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-tab-pane>

      <!-- 登录日志 -->
      <el-tab-pane label="登录日志" name="logs">
        <div class="logs-section">
          <el-table :data="loginLogs" v-loading="loadingLogs" stripe>
            <el-table-column prop="loginTime" label="登录时间" width="180" />
            <el-table-column prop="loginIp" label="登录IP" width="150" />
            <el-table-column prop="deviceType" label="设备类型" width="100" />
            <el-table-column prop="deviceInfo" label="设备信息" />
            <el-table-column prop="location" label="登录地点" width="150" />
            <el-table-column prop="isSuccess" label="状态" width="80">
              <template #default>
                <el-tag type="info" size="small">-</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="logPage"
              v-model:page-size="logPageSize"
              :total="logTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, prev, pager, next"
              @current-change="loadLoginLogs"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 绑定手机号对话框 -->
    <el-dialog v-model="showBindPhoneDialog" title="绑定手机号" width="400px">
      <el-form :model="phoneForm" :rules="phoneRules" ref="phoneFormRef" label-width="100px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="phoneForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="验证码" prop="verifyCode">
          <el-input v-model="phoneForm.verifyCode" placeholder="请输入验证码">
            <template #append>
              <el-button @click="sendVerifyCode" :disabled="countdown > 0">
                {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBindPhoneDialog = false">取消</el-button>
        <el-button type="primary" @click="handleBindPhone" :loading="bindingPhone">确定</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { Check, Close, Lock, Phone, Message } from '@element-plus/icons-vue';
import { useAuthStore } from '../stores/auth';

const defaultAvatar = '/logo/小LOGO.png';
const activeTab = ref('basic');

function getAuthHeaders(): Record<string, string> {
  const auth = useAuthStore();
  return {
    'X-Request-Id': crypto.randomUUID(),
    'X-Tenant-Id': auth.tenantId,
    'Authorization': `Bearer ${auth.accessToken}`
  };
}

const profile = reactive({
  id: 0,
  nickname: '',
  avatarUrl: '',
  mobile: '',
  phone: '',
  email: '',
  wechatBound: false,
  wechatOpenid: '',
  wechatUnionid: '',
  roleCode: '',
  status: 1
});

const formData = reactive({
  nickname: '',
  avatarUrl: '',
  email: ''
});

const security = reactive({
  hasPassword: false,
  hasPhone: false,
  hasWechat: false,
  hasEmail: false,
  lastLoginTime: '',
  lastLoginIp: '',
  loginDeviceCount: 0
});

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const phoneForm = reactive({
  phone: '',
  verifyCode: ''
});

const loginLogs = ref<any[]>([]);
const logPage = ref(1);
const logPageSize = ref(20);
const logTotal = ref(0);

const formRules: FormRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 50, message: '昵称长度为 1-50 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
};

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度为 6-50 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

const phoneRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
};

const loading = ref(false);
const bindingPhone = ref(false);
const changingPassword = ref(false);
const loadingLogs = ref(false);
const showBindPhoneDialog = ref(false);
const countdown = ref(0);
const fileInput = ref<HTMLInputElement | null>(null);
const formRef = ref<FormInstance | null>(null);
const passwordFormRef = ref<FormInstance | null>(null);
const phoneFormRef = ref<FormInstance | null>(null);

onMounted(() => {
  loadProfile();
  loadSecurity();
  loadLoginLogs();
});

async function loadProfile() {
  try {
    const response = await fetch('/api/v1/user/profile', {
      headers: getAuthHeaders()
    });
    const payload = await response.json();
    if (payload.code === 0) {
      Object.assign(profile, payload.data);
      Object.assign(formData, {
        nickname: payload.data.nickname,
        avatarUrl: payload.data.avatarUrl,
        email: payload.data.email
      });
    }
  } catch (error) {
    ElMessage.error('加载个人信息失败');
  }
}

async function loadSecurity() {
  try {
    const response = await fetch('/api/v1/user/security', {
      headers: getAuthHeaders()
    });
    const payload = await response.json();
    if (payload.code === 0) {
      Object.assign(security, payload.data);
    }
  } catch (error) {
    console.error('加载安全信息失败', error);
  }
}

async function loadLoginLogs() {
  loadingLogs.value = true;
  try {
    const response = await fetch(`/api/v1/user/login-logs?page=${logPage.value}&pageSize=${logPageSize.value}`, {
      headers: getAuthHeaders()
    });
    const payload = await response.json();
    if (payload.code === 0) {
      loginLogs.value = payload.data.logs;
      logTotal.value = payload.data.total;
    }
  } catch (error) {
    ElMessage.error('加载登录日志失败');
  } finally {
    loadingLogs.value = false;
  }
}

async function handleSaveProfile() {
  if (!formRef.value) {
    ElMessage.error('表单加载中，请稍后');
    return;
  }

  try {
    await formRef.value.validate();
  } catch (e) {
    return;
  }

  loading.value = true;
  try {
    const response = await fetch('/api/v1/user/profile', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeaders()
      },
      body: JSON.stringify(formData)
    });
    const payload = await response.json();
    if (payload.code === 0) {
      ElMessage.success('保存成功');
      await loadProfile();
    } else {
      ElMessage.error(payload.message || '保存失败');
    }
  } catch (error) {
    ElMessage.error('保存失败');
  } finally {
    loading.value = false;
  }
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return;
  try {
    await passwordFormRef.value.validate();
  } catch (e) {
    return;
  }

  changingPassword.value = true;
  try {
    const response = await fetch('/api/v1/user/change-password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeaders()
      },
      body: JSON.stringify(passwordForm)
    });
    const payload = await response.json();
    if (payload.code === 0) {
      ElMessage.success('密码修改成功');
      passwordForm.oldPassword = '';
      passwordForm.newPassword = '';
      passwordForm.confirmPassword = '';
      await loadSecurity();
    } else {
      ElMessage.error(payload.message || '密码修改失败');
    }
  } catch (error) {
    ElMessage.error('密码修改失败');
  } finally {
    changingPassword.value = false;
  }
}

function handleUploadAvatar() {
  fileInput.value?.click();
}

async function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  const uploadFormData = new FormData();
  uploadFormData.append('file', file);

  try {
    const response = await fetch('/api/v1/user/upload-avatar', {
      method: 'POST',
      headers: getAuthHeaders(),
      body: uploadFormData
    });
    const payload = await response.json();
    if (payload.code === 0) {
      ElMessage.success('头像上传成功');
      await loadProfile();
    } else {
      ElMessage.error(payload.message || '上传失败');
    }
  } catch (error) {
    ElMessage.error('上传失败');
  }

  target.value = '';
}

async function handleBindPhone() {
  if (!phoneFormRef.value) return;
  try {
    await phoneFormRef.value.validate();
  } catch (e) {
    return;
  }

  bindingPhone.value = true;
  try {
    const response = await fetch('/api/v1/user/bind/phone', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeaders()
      },
      body: JSON.stringify(phoneForm)
    });
    const payload = await response.json();
    if (payload.code === 0) {
      ElMessage.success('绑定成功');
      showBindPhoneDialog.value = false;
      await loadProfile();
      await loadSecurity();
    } else {
      ElMessage.error(payload.message || '绑定失败');
    }
  } catch (error) {
    ElMessage.error('绑定失败');
  } finally {
    bindingPhone.value = false;
  }
}

function sendVerifyCode() {
  ElMessage.info('验证码功能待实现');
}

async function handleBindWechat() {
  ElMessage.info('微信绑定功能待实现（需要小程序端调用）');
}

async function handleUnbindWechat() {
  try {
    const response = await fetch('/api/v1/user/unbind/wechat', {
      method: 'POST',
      headers: getAuthHeaders()
    });
    const payload = await response.json();
    if (payload.code === 0) {
      ElMessage.success('解绑成功');
      await loadProfile();
      await loadSecurity();
    } else {
      ElMessage.error(payload.message || '解绑失败');
    }
  } catch (error) {
    ElMessage.error('解绑失败');
  }
}

function getRoleName(roleCode: string): string {
  const roleNames: Record<string, string> = {
    'boss': '老板',
    'tenant_admin': '管理员',
    'tenant_operator': '操作员',
    'tenant_viewer': '查看者',
    'platform_super_admin': '平台管理员'
  };
  return roleNames[roleCode] || roleCode;
}
</script>

<style scoped>
.profile-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
  color: hsl(var(--foreground));
}

.profile-container h1 {
  margin-bottom: 30px;
  font-size: 28px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 30px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 40px;
}

.avatar-section {
  background: hsl(var(--card));
  border-radius: 10px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(18, 30, 61, 0.08);
  border: 1px solid hsl(var(--border));
  height: fit-content;
}

.avatar-wrapper {
  text-align: center;
  margin-bottom: 20px;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid hsl(var(--border));
}

.avatar-actions {
  margin-top: 15px;
}

.user-info {
  text-align: center;
  margin-bottom: 30px;
}

.user-info h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.user-role {
  margin: 0;
  color: hsl(var(--muted-foreground));
  font-size: 14px;
}

.binding-status {
  border-top: 1px solid hsl(var(--border));
  padding-top: 20px;
}

.binding-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  color: hsl(var(--muted-foreground));
  font-size: 14px;
}

.binding-item .el-icon {
  margin-right: 8px;
  color: hsl(var(--destructive));
}

.binding-item .el-icon.bound {
  color: hsl(var(--success));
}

.form-section {
  background: hsl(var(--card));
  border-radius: 10px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(18, 30, 61, 0.08);
  border: 1px solid hsl(var(--border));
}

/* 安全设置样式 */
.security-section {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.security-overview,
.password-section,
.logs-section {
  background: hsl(var(--card));
  border-radius: 10px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(18, 30, 61, 0.08);
  border: 1px solid hsl(var(--border));
}

.security-overview h3,
.password-section h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: hsl(var(--accent));
  border-radius: 8px;
}

.overview-item .el-icon {
  font-size: 20px;
  color: hsl(var(--primary));
}

.overview-item span {
  flex: 1;
  font-size: 14px;
}

.login-info {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid hsl(var(--border));
  color: hsl(var(--muted-foreground));
  font-size: 14px;
}

.login-info p {
  margin: 8px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
