<template>
  <section class="settings-container">
    <h1>设置</h1>

    <el-tabs v-model="activeTab" class="settings-tabs">
      <!-- 个人中心 -->
      <el-tab-pane label="个人中心" name="profile">
        <div class="settings-section">
          <div class="profile-center">
            <div class="user-info">
              <h2>{{ profileForm.nickname || '未设置昵称' }}</h2>
              <p class="user-role">{{ getRoleName(auth.roleCode) }}</p>
            </div>
          </div>
          <div class="profile-form-area">
            <el-form :model="profileForm" :rules="profileRules" ref="profileFormRef" label-width="100px" style="max-width: 500px; margin: 0 auto;">
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="50" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="profileForm.mobile" disabled />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleSaveProfile" :loading="savingProfile">保存</el-button>
              </el-form-item>
            </el-form>
            <h3 style="margin-top: 24px; padding-top: 24px; border-top: 1px solid hsl(var(--border)); max-width: 500px; margin-left: auto; margin-right: auto;">账号绑定</h3>
            <!-- 更换手机号码 -->
            <div class="binding-section" style="max-width: 500px; margin: 0 auto 20px auto;">
              <div class="binding-row">
                <div class="binding-info">
                  <span class="binding-label">手机号码</span>
                  <span class="binding-value">{{ profileForm.mobile || '未绑定' }}</span>
                </div>
                <el-button type="primary" plain size="small" @click="showMobileDialog = true">更换</el-button>
              </div>
            </div>
            <!-- 微信绑定 -->
            <div class="binding-section" style="max-width: 500px; margin: 0 auto;">
              <div class="binding-row">
                <div class="binding-info">
                  <span class="binding-label">微信账号</span>
                  <span class="binding-value">{{ wechatBound ? '已绑定' : '未绑定' }}</span>
                </div>
                <el-button v-if="!wechatBound" type="success" size="small" @click="handleWechatBind">绑定微信</el-button>
                <el-button v-else type="danger" plain size="small" @click="handleWechatUnbind">解绑</el-button>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 通知设置 -->
      <el-tab-pane label="通知设置" name="notifications">
        <div class="settings-section">
          <h3>通知偏好</h3>
          <div class="notification-list">
            <div class="notification-item">
              <div class="notification-info">
                <h4>登录通知</h4>
                <p>异地登录时发送通知</p>
              </div>
              <el-switch v-model="notifications.loginNotification" @change="handleNotificationChange" />
            </div>
            <div class="notification-item">
              <div class="notification-info">
                <h4>账单变动</h4>
                <p>余额变动、扣款时发送通知</p>
              </div>
              <el-switch v-model="notifications.billingNotification" @change="handleNotificationChange" />
            </div>
            <div class="notification-item">
              <div class="notification-info">
                <h4>成员变动</h4>
                <p>成员加入、退出时发送通知</p>
              </div>
              <el-switch v-model="notifications.memberNotification" @change="handleNotificationChange" />
            </div>
            <div class="notification-item">
              <div class="notification-info">
                <h4>插件使用</h4>
                <p>AI 插件生成完成时发送通知</p>
              </div>
              <el-switch v-model="notifications.pluginNotification" @change="handleNotificationChange" />
            </div>
          </div>

          <h3>通知渠道</h3>
          <div class="channel-list">
            <div class="channel-item">
              <div class="channel-info">
                <el-icon><Message /></el-icon>
                <span>短信通知</span>
              </div>
              <el-switch v-model="channels.sms" @change="handleNotificationChange" />
            </div>
            <div class="channel-item">
              <div class="channel-info">
                <el-icon><ChatDotRound /></el-icon>
                <span>微信服务号通知</span>
              </div>
              <el-switch v-model="channels.wechat" @change="handleNotificationChange" />
            </div>
            <div class="channel-item">
              <div class="channel-info">
                <el-icon><MessageBox /></el-icon>
                <span>站内消息</span>
              </div>
              <el-switch v-model="channels.inApp" @change="handleNotificationChange" />
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 企业信息 -->
      <el-tab-pane label="企业信息" name="tenant">
        <div class="settings-section">
          <el-form :model="tenantForm" :rules="tenantRules" ref="tenantFormRef" label-width="120px" style="max-width: 600px">
            <el-form-item label="企业名称" prop="name">
              <el-input v-model="tenantForm.name" placeholder="请输入企业名称" maxlength="100" />
            </el-form-item>
            <el-form-item label="企业Logo">
              <div class="logo-upload">
                <img :src="tenantForm.logo || defaultLogo" class="logo-preview" />
                <el-button size="small" @click="handleUploadLogo">更换Logo</el-button>
                <input type="file" ref="logoInput" accept="image/*" style="display: none" @change="handleLogoChange" />
              </div>
            </el-form-item>
            <el-form-item label="行业分类" prop="industry">
              <el-select v-model="tenantForm.industry" placeholder="请选择行业分类" style="width: 100%">
                <el-option label="服装纺织" value="textile" />
                <el-option label="毛衫针织" value="knitwear" />
                <el-option label="电商零售" value="retail" />
                <el-option label="设计服务" value="design" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="tenantForm.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
            <el-form-item label="联系邮箱" prop="contactEmail">
              <el-input v-model="tenantForm.contactEmail" placeholder="请输入联系邮箱" />
            </el-form-item>
            <el-form-item label="地址" prop="address">
              <el-input v-model="tenantForm.address" type="textarea" :rows="3" placeholder="请输入详细地址" />
            </el-form-item>
            <el-form-item label="简介" prop="description">
              <el-input v-model="tenantForm.description" type="textarea" :rows="4" placeholder="请输入企业简介" maxlength="500" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveTenant" :loading="savingTenant">保存</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 创建新工作室 -->
        <div class="settings-section" style="margin-top: 24px;">
          <h3>工作室管理</h3>
          <p style="color: hsl(var(--muted-foreground)); font-size: 14px; margin: 0 0 16px;">
            您当前属于 {{ auth.tenants.length }} 个工作室。可以创建新工作室或切换到其他工作室。
          </p>
          <div class="studio-list" v-if="auth.tenants.length > 0">
            <div
              v-for="tenant in auth.tenants"
              :key="tenant.tenantId"
              :class="['studio-item', { active: String(tenant.tenantId) === auth.tenantId }]"
            >
              <span class="studio-icon">🏢</span>
              <span class="studio-info">
                <span class="studio-name">{{ tenant.tenantName }}</span>
                <span class="studio-role">{{ getRoleName(tenant.role) }}</span>
              </span>
              <el-tag v-if="String(tenant.tenantId) === auth.tenantId" type="success" size="small">当前</el-tag>
              <el-button v-else type="primary" plain size="small" @click="handleSwitchTenant(tenant.tenantId)">切换</el-button>
            </div>
          </div>
          <div class="create-studio-form" style="margin-top: 16px; padding-top: 16px; border-top: 1px solid hsl(var(--border));">
            <h4 style="margin: 0 0 12px; font-size: 14px; color: hsl(var(--foreground));">创建新工作室</h4>
            <div style="display: flex; gap: 10px; max-width: 500px;">
              <el-input v-model="newStudioName" placeholder="请输入工作室名称（至少2个字）" maxlength="100" @keyup.enter="handleCreateStudio" />
              <el-button type="primary" @click="handleCreateStudio" :loading="creatingStudio" :disabled="newStudioName.trim().length < 2">
                {{ creatingStudio ? '创建中' : '创建' }}
              </el-button>
            </div>
            <p v-if="createStudioError" style="color: hsl(var(--destructive)); font-size: 13px; margin-top: 8px;">{{ createStudioError }}</p>
            <p style="color: hsl(var(--muted-foreground)); font-size: 12px; margin-top: 8px;">
              💡 创建后您将自动成为该工作室的管理员，可以邀请成员加入。
            </p>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
    <!-- 更换手机号弹窗 -->
    <el-dialog v-model="showMobileDialog" title="更换手机号码" width="420px" :close-on-click-modal="false">
      <el-form :model="mobileForm" :rules="mobileRules" ref="mobileFormRef" label-width="80px">
        <el-form-item label="新手机号" prop="newMobile">
          <el-input v-model="mobileForm.newMobile" placeholder="请输入新手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="验证码" prop="smsCode">
          <div style="display: flex; gap: 10px; width: 100%;">
            <el-input v-model="mobileForm.smsCode" placeholder="请输入验证码" maxlength="6" style="flex: 1;" />
            <el-button :disabled="smsCountdown > 0" @click="handleSendSms" :loading="sendingSms" style="flex-shrink: 0;">
              {{ smsCountdown > 0 ? `${smsCountdown}s后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showMobileDialog = false">取消</el-button>
        <el-button type="primary" @click="handleChangeMobile" :loading="changingMobile">确认更换</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { Message, ChatDotRound, MessageBox } from '@element-plus/icons-vue';
import { useAuthStore } from '../stores/auth';
import { generateUUID } from '../utils/uuid';
import { useRouter } from 'vue-router';

const defaultLogo = '/logo/small-logo.png';
const activeTab = ref('profile');
const auth = useAuthStore();
const router = useRouter();

const profile = reactive({
  nickname: '',
  avatarUrl: '',
  mobile: '',
  email: '',
});

const profileForm = reactive({
  nickname: '',
});

const profileRules = reactive<FormRules>({
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 50, message: '昵称长度为 1-50 个字符', trigger: 'blur' }
  ],
});

const savingProfile = ref(false);
const profileFormRef = ref<FormInstance | null>(null);

async function loadProfile() {
  try {
    const response = await fetch('/api/v1/user/profile', {
      headers: getAuthHeaders()
    });
    const payload = await response.json();
    if (payload.code === 0) {
      Object.assign(profile, payload.data);
      profileForm.nickname = payload.data.nickname;
    }
  } catch (error) {
    ElMessage.error('加载个人信息失败');
  }
}

async function handleSaveProfile() {
  if (!profileFormRef.value) {
    ElMessage.error('表单加载中，请稍后');
    return;
  }
  try {
    await profileFormRef.value.validate();
  } catch (e) {
    return;
  }
  savingProfile.value = true;
  try {
    const response = await fetch('/api/v1/user/profile', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeaders()
      },
      body: JSON.stringify(profileForm)
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
    savingProfile.value = false;
  }
}

function getAuthHeaders(): Record<string, string> {
  const auth = useAuthStore();
  return {
    'X-Request-Id': generateUUID(),
    'X-Tenant-Id': auth.tenantId,
    'Authorization': `Bearer ${auth.accessToken}`
  };
}

const notifications = reactive({
  loginNotification: true,
  billingNotification: true,
  memberNotification: false,
  pluginNotification: true
});

const channels = reactive({
  sms: true,
  wechat: false,
  inApp: true
});

const tenantForm = reactive({
  name: '',
  logo: '',
  industry: '',
  contactPhone: '',
  contactEmail: '',
  address: '',
  description: ''
});

const tenantRules = {
  contactEmail: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
};

const savingTenant = ref(false);
const logoInput = ref<HTMLInputElement | null>(null);

// ===== 更换手机号码 =====
const showMobileDialog = ref(false);
const changingMobile = ref(false);
const sendingSms = ref(false);
const smsCountdown = ref(0);
let smsTimer: ReturnType<typeof setInterval> | null = null;

const mobileForm = reactive({
  newMobile: '',
  smsCode: ''
});

const mobileRules = reactive<FormRules>({
  newMobile: [
    { required: true, message: '请输入新手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  smsCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
});

const mobileFormRef = ref<FormInstance | null>(null);

async function handleSendSms() {
  if (!mobileForm.newMobile || !/^1[3-9]\d{9}$/.test(mobileForm.newMobile)) {
    ElMessage.warning('请先输入正确的手机号');
    return;
  }
  sendingSms.value = true;
  try {
    const response = await fetch('/api/v1/sms/send-code', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeaders()
      },
      body: JSON.stringify({ mobile: mobileForm.newMobile, scene: 'change_mobile' })
    });
    const payload = await response.json();
    if (payload.code === 0) {
      ElMessage.success('验证码已发送');
      smsCountdown.value = 60;
      smsTimer = setInterval(() => {
        smsCountdown.value--;
        if (smsCountdown.value <= 0) {
          if (smsTimer) clearInterval(smsTimer);
          smsTimer = null;
        }
      }, 1000);
    } else {
      ElMessage.error(payload.message || '发送验证码失败');
    }
  } catch (error) {
    ElMessage.error('发送验证码失败');
  } finally {
    sendingSms.value = false;
  }
}

async function handleChangeMobile() {
  if (!mobileFormRef.value) return;
  try {
    await mobileFormRef.value.validate();
  } catch (e) {
    return;
  }
  changingMobile.value = true;
  try {
    const response = await fetch('/api/v1/user/change-mobile', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeaders()
      },
      body: JSON.stringify({ newMobile: mobileForm.newMobile, smsCode: mobileForm.smsCode })
    });
    const payload = await response.json();
    if (payload.code === 0) {
      ElMessage.success('手机号更换成功');
      showMobileDialog.value = false;
      mobileForm.newMobile = '';
      mobileForm.smsCode = '';
      await loadProfile();
    } else {
      ElMessage.error(payload.message || '手机号更换失败');
    }
  } catch (error) {
    ElMessage.error('手机号更换失败');
  } finally {
    changingMobile.value = false;
  }
}

// ===== 微信绑定 =====
const wechatBound = ref(false);

async function loadWechatStatus() {
  try {
    const response = await fetch('/api/v1/user/wechat-status', {
      headers: getAuthHeaders()
    });
    const payload = await response.json();
    if (payload.code === 0) {
      wechatBound.value = payload.data?.bound ?? false;
    }
  } catch (error) {
    console.error('加载微信绑定状态失败', error);
  }
}

async function handleWechatBind() {
  try {
    const response = await fetch('/api/v1/user/wechat-bind-url', {
      headers: getAuthHeaders()
    });
    const payload = await response.json();
    if (payload.code === 0 && payload.data?.url) {
      window.open(payload.data.url, '_blank', 'width=600,height=700');
      // 轮询检查绑定状态
      ElMessage.info('请在打开的窗口中扫码绑定微信，绑定完成后将自动刷新状态');
      setTimeout(() => loadWechatStatus(), 3000);
    } else {
      ElMessage.error(payload.message || '获取绑定链接失败');
    }
  } catch (error) {
    ElMessage.error('获取绑定链接失败');
  }
}

async function handleWechatUnbind() {
  try {
    const response = await fetch('/api/v1/user/wechat-unbind', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeaders()
      }
    });
    const payload = await response.json();
    if (payload.code === 0) {
      ElMessage.success('微信已解绑');
      wechatBound.value = false;
    } else {
      ElMessage.error(payload.message || '解绑失败');
    }
  } catch (error) {
    ElMessage.error('解绑失败');
  }
}

function getRoleName(roleCode: string | null): string {
  const roleNames: Record<string, string> = {
    'boss': '老板',
    'tenant_admin': '管理员',
    'tenant_operator': '操作员',
    'tenant_viewer': '查看者',
    'platform_super_admin': '平台管理员'
  };
  return roleNames[roleCode || ''] || roleCode || '';
}

const newStudioName = ref('');
const creatingStudio = ref(false);
const createStudioError = ref('');

async function handleCreateStudio() {
  const name = newStudioName.value.trim();
  if (name.length < 2) {
    createStudioError.value = '工作室名称至少需要2个字';
    return;
  }
  creatingStudio.value = true;
  createStudioError.value = '';
  try {
    const result = await auth.createTenant(name);
    if (result.success) {
      ElMessage.success(`工作室「${name}」创建成功`);
      newStudioName.value = '';
      await auth.loadTenants();
    } else {
      createStudioError.value = result.message || '创建失败';
    }
  } catch (e: any) {
    createStudioError.value = e.message || '创建失败';
  } finally {
    creatingStudio.value = false;
  }
}

async function handleSwitchTenant(tenantId: number) {
  try {
    const result = await auth.switchTenant(tenantId);
    if (result.success) {
      ElMessage.success('已切换工作室');
      await loadTenantInfo();
    } else {
      ElMessage.error(result.message || '切换失败');
    }
  } catch (e: any) {
    ElMessage.error(e.message || '切换失败');
  }
}

onMounted(() => {
  loadProfile();
  loadSettings();
  loadTenantInfo();
  loadWechatStatus();
});

async function loadSettings() {
  const saved = localStorage.getItem('notification_settings');
  if (saved) {
    try {
      const parsed = JSON.parse(saved);
      Object.assign(notifications, parsed.notifications || {});
      Object.assign(channels, parsed.channels || {});
    } catch (e) {}
  }
}

async function loadTenantInfo() {
  try {
    const response = await fetch('/api/v1/tenant/profile', {
      headers: getAuthHeaders()
    });
    const payload = await response.json();
    if (payload.code === 0) {
      tenantForm.name = payload.data.tenant_name || '';
      tenantForm.logo = payload.data.logo_url || '';
      tenantForm.industry = payload.data.industry || '';
      tenantForm.contactPhone = payload.data.contact_phone || '';
      tenantForm.contactEmail = payload.data.contact_email || '';
      tenantForm.address = payload.data.address || '';
      tenantForm.description = payload.data.description || '';
    }
  } catch (error) {
    console.error('加载企业信息失败', error);
  }
}

function handleNotificationChange() {
  localStorage.setItem('notification_settings', JSON.stringify({
    notifications,
    channels
  }));
  ElMessage.success('设置已保存');
}

async function handleSaveTenant() {
  savingTenant.value = true;
  try {
    const response = await fetch('/api/v1/tenant/profile', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeaders()
      },
      body: JSON.stringify({
        name: tenantForm.name,
        logo_url: tenantForm.logo,
        industry: tenantForm.industry,
        contact_phone: tenantForm.contactPhone,
        contact_email: tenantForm.contactEmail,
        address: tenantForm.address,
        description: tenantForm.description
      })
    });
    const payload = await response.json();
    if (payload.code === 0) {
      ElMessage.success('企业信息已保存');
      auth.enterpriseName = tenantForm.name;
      localStorage.setItem('merchant_enterprise_name', tenantForm.name);
      await auth.loadProfile();
    } else {
      ElMessage.error(payload.message || '保存失败');
    }
  } catch (error) {
    ElMessage.error('保存失败');
  } finally {
    savingTenant.value = false;
  }
}

function handleUploadLogo() {
  logoInput.value?.click();
}

async function handleLogoChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过5MB');
    target.value = '';
    return;
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.error('仅支持图片文件');
    target.value = '';
    return;
  }

  try {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch('/api/v1/tenant/upload-logo', {
      method: 'POST',
      headers: {
        ...getAuthHeaders()
      },
      body: formData
    });
    const payload = await response.json();
    if (payload.code === 0) {
      tenantForm.logo = payload.data.url;
      await auth.loadProfile();
      ElMessage.success('Logo上传成功');
    } else {
      ElMessage.error(payload.message || 'Logo上传失败');
    }
  } catch (error) {
    ElMessage.error('Logo上传失败');
  } finally {
    target.value = '';
  }
}
</script>

<style scoped>
.settings-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 40px 20px;
  color: hsl(var(--foreground));
}

.settings-container h1 {
  margin-bottom: 30px;
  font-size: 28px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.settings-tabs :deep(.el-tabs__header) {
  margin-bottom: 30px;
}

.settings-section {
  background: hsl(var(--card));
  border-radius: 10px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(18, 30, 61, 0.08);
  border: 1px solid hsl(var(--border));
}

.settings-section h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.settings-section h3:not(:first-child) {
  margin-top: 30px;
  padding-top: 30px;
  border-top: 1px solid hsl(var(--border));
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notification-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: hsl(var(--accent));
  border-radius: 8px;
}

.notification-info h4 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 500;
  color: hsl(var(--foreground));
}

.notification-info p {
  margin: 0;
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}

.channel-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.channel-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: hsl(var(--accent));
  border-radius: 8px;
}

.channel-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.channel-info .el-icon {
  font-size: 18px;
  color: hsl(var(--primary));
}

.logo-upload {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-preview {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid hsl(var(--border));
}

/* 个人中心 Tab 样式 */
.profile-center {
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid hsl(var(--border));
}

.profile-form-area {
  width: 100%;
}

.user-info {
  margin-bottom: 16px;
}

.user-info h2 {
  margin: 0 0 6px 0;
  font-size: 18px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.user-role {
  margin: 0;
  color: hsl(var(--muted-foreground));
  font-size: 14px;
}

.form-section h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

/* 账号绑定区域 */
.binding-section {
  padding: 0;
}

.binding-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: hsl(var(--accent));
  border-radius: 8px;
  gap: 16px;
}

.binding-info {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.binding-label {
  font-size: 14px;
  font-weight: 500;
  color: hsl(var(--foreground));
  white-space: nowrap;
}

.binding-value {
  font-size: 14px;
  color: hsl(var(--muted-foreground));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}

.studio-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.studio-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  background: hsl(var(--card));
}

.studio-item.active {
  border-color: hsl(var(--primary) / 0.4);
  background: hsl(var(--primary) / 0.05);
}

.studio-icon {
  font-size: 20px;
}

.studio-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.studio-name {
  font-weight: 600;
  color: hsl(var(--foreground));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.studio-role {
  font-size: 13px;
  color: hsl(var(--muted-foreground));
}
</style>
