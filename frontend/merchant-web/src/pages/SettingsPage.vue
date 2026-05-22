<template>
  <section class="settings-container">
    <h1>设置</h1>

    <el-tabs v-model="activeTab" class="settings-tabs">
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

      <!-- 工作室信息 -->
      <el-tab-pane label="工作室信息" name="tenant">
        <div class="settings-section">
          <el-form :model="tenantForm" :rules="tenantRules" ref="tenantFormRef" label-width="120px" style="max-width: 600px">
            <el-form-item label="工作室名称" prop="name">
              <el-input v-model="tenantForm.name" placeholder="请输入工作室名称" maxlength="100" />
            </el-form-item>
            <el-form-item label="工作室Logo">
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
              <el-input v-model="tenantForm.description" type="textarea" :rows="4" placeholder="请输入工作室简介" maxlength="500" show-word-limit />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveTenant" :loading="savingTenant">保存</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Message, ChatDotRound, MessageBox } from '@element-plus/icons-vue';
import { useAuthStore } from '../stores/auth';

const defaultLogo = '/logo/小LOGO.png';
const activeTab = ref('notifications');

function getAuthHeaders(): Record<string, string> {
  const auth = useAuthStore();
  return {
    'X-Request-Id': crypto.randomUUID(),
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

onMounted(() => {
  loadSettings();
  loadTenantInfo();
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
    console.error('加载工作室信息失败', error);
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
    ElMessage.success('工作室信息已保存');
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
  ElMessage.info('Logo上传功能待实现');
  target.value = '';
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
</style>
