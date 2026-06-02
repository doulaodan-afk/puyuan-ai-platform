<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()

const formData = ref({
  companyName: '',
  contactName: '',
  contactMobile: '',
  address: '',
  fabricCategories: [] as string[],
  description: ''
})

const businessLicense = ref<File | null>(null)
const submitting = ref(false)

const fabricCategoryOptions = [
  { label: '真丝', value: '真丝' },
  { label: '羊毛', value: '羊毛' },
  { label: '棉麻', value: '棉麻' },
  { label: '化纤', value: '化纤' },
  { label: '混纺', value: '混纺' }
]

function handleFileUpload(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files && input.files[0]) {
    businessLicense.value = input.files[0]
  }
}

async function submitRegistration() {
  if (!businessLicense.value) {
    ElMessage.warning('请上传营业执照')
    return
  }

  submitting.value = true

  try {
    const licenseUrl = 'https://example.com/license/' + businessLicense.value.name

    const response = await fetch('/api/v1/supplier/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        company_name: formData.value.companyName,
        contact_name: formData.value.contactName,
        contact_mobile: formData.value.contactMobile,
        business_license: licenseUrl,
        address: formData.value.address,
        fabric_categories: formData.value.fabricCategories,
        description: formData.value.description
      })
    })

    const result = await response.json()
    if (result.code === 0) {
      ElMessage.success('入驻申请提交成功，请等待审核')
      router.push('/login')
    } else {
      ElMessage.error(result.message || '提交失败')
    }
  } catch (error) {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="supplier-register-page page-container">
    <div class="register-container">
      <div class="register-header">
        <h1>面料商入驻申请</h1>
        <p>填写以下信息提交入驻申请</p>
      </div>

      <el-form :model="formData" label-width="120px" class="register-form">
        <el-form-item label="公司名称" required>
          <el-input v-model="formData.companyName" placeholder="请输入公司名称" />
        </el-form-item>

        <el-form-item label="联系人姓名" required>
          <el-input v-model="formData.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>

        <el-form-item label="联系人手机" required>
          <el-input v-model="formData.contactMobile" placeholder="请输入联系人手机号" />
        </el-form-item>

        <el-form-item label="公司地址">
          <el-input v-model="formData.address" placeholder="请输入公司地址" />
        </el-form-item>

        <el-form-item label="面料品类">
          <el-checkbox-group v-model="formData.fabricCategories">
            <el-checkbox
              v-for="option in fabricCategoryOptions"
              :key="option.value"
              :label="option.value"
            >
              {{ option.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="公司介绍">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请简要介绍公司业务和优势"
          />
        </el-form-item>

        <el-form-item label="营业执照" required>
          <el-upload
            class="license-upload"
            :auto-upload="false"
            :on-change="handleFileUpload"
            :show-file-list="false"
            accept="image/*"
          >
            <div v-if="!businessLicense" class="upload-placeholder">
              <span class="upload-icon">📄</span>
              <p>点击上传营业执照</p>
            </div>
            <div v-else class="upload-preview">
              <span>{{ businessLicense.name }}</span>
              <el-button type="danger" size="small" @click.stop="businessLicense = null">
                删除
              </el-button>
            </div>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="submitRegistration"
            :loading="submitting"
            style="width: 100%"
          >
            {{ submitting ? '提交中...' : '提交申请' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <p>已有账号？<router-link to="/login">立即登录</router-link></p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.supplier-register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.register-container {
  width: 100%;
  max-width: 600px;
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-header h1 {
  margin: 0 0 8px;
  font-size: 28px;
  color: #1f2937;
}

.register-header p {
  margin: 0;
  color: #6b7280;
}

.register-form {
  margin-bottom: 24px;
}

.license-upload {
  width: 100%;
}

.upload-placeholder {
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-placeholder:hover {
  border-color: #10a37f;
  background: #f9fafb;
}

.upload-icon {
  font-size: 32px;
  display: block;
  margin-bottom: 8px;
}

.upload-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f3f4f6;
  border-radius: 8px;
}

.register-footer {
  text-align: center;
  color: #6b7280;
}

.register-footer a {
  color: #10a37f;
  text-decoration: none;
}
</style>