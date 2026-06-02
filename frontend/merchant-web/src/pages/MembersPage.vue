<template>
  <section class="members-container">
    <div class="page-header">
      <h1>成员管理</h1>
      <el-button type="primary" @click="showInviteDialog = true">
        <el-icon><Plus /></el-icon>
        添加成员
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索成员"
        style="width: 200px"
        clearable
        @clear="loadMembers"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select v-model="filters.status" placeholder="状态" clearable @change="loadMembers">
        <el-option label="启用" value="active" />
        <el-option label="禁用" value="inactive" />
      </el-select>

      <el-select v-model="filters.roleCode" placeholder="角色" clearable @change="loadMembers">
        <el-option
          v-for="role in roles"
          :key="role.roleCode"
          :label="role.roleName"
          :value="role.roleCode"
        />
      </el-select>

      <el-button @click="loadMembers">搜索</el-button>
    </div>

    <!-- 成员列表 -->
    <el-table :data="members" v-loading="loading" stripe>
      <el-table-column label="成员" width="280">
        <template #default="{ row }">
          <div class="member-cell">
            <img :src="row.avatarUrl || defaultAvatar" class="avatar" />
            <div class="member-info">
              <div class="name">{{ row.nickname || '未设置' }}</div>
              <div class="contact">{{ row.phone || row.mobile || row.email || '-' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="roleName" label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="getRoleTagType(getRoleCode(row))">
            {{ row.roleName || getRoleCode(row) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
            {{ row.status === 'active' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="creatorName" label="邀请人" width="120" />

      <el-table-column prop="createdAt" label="加入时间" width="180" />

      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link @click="handleEditRole(row)">修改角色</el-button>
          <el-button
            :type="getStatus(row) === 'active' ? 'warning' : 'success'"
            link
            @click="handleToggleStatus(row)"
          >
            {{ getStatus(row) === 'active' ? '禁用' : '启用' }}
          </el-button>
          <el-button type="danger" link @click="handleRemove(row)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadMembers"
        @current-change="loadMembers"
      />
    </div>

    <!-- 邀请成员对话框 -->
    <el-dialog v-model="showInviteDialog" title="添加成员" width="500px">
      <el-form :model="inviteForm" :rules="inviteRules" ref="inviteFormRef" label-width="100px">
        <el-form-item label="联系方式" prop="contact">
          <el-input
            v-model="inviteForm.contact"
            placeholder="请输入手机号或邮箱"
            clearable
          />
        </el-form-item>

        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="inviteForm.roleCode" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roles.filter(r => !r.isSystem || r.roleCode !== 'boss')"
              :key="role.roleCode"
              :label="role.roleName"
              :value="role.roleCode"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="inviteForm.remark"
            type="textarea"
            :rows="3"
            placeholder="可选：添加备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInviteDialog = false">取消</el-button>
        <el-button type="primary" @click="handleInvite" :loading="inviting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改角色对话框 -->
    <el-dialog v-model="showRoleDialog" title="修改角色" width="400px">
      <el-form :model="roleForm" :rules="roleRules" ref="roleFormRef" label-width="100px">
        <el-form-item label="角色" prop="roleCode">
          <el-select v-model="roleForm.roleCode" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roles.filter(r => !r.isSystem || r.roleCode !== 'boss')"
              :key="role.roleCode"
              :label="role.roleName"
              :value="role.roleCode"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRoleDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRole" :loading="savingRole">确定</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Plus, Search } from '@element-plus/icons-vue';
import { useAuthStore } from '../stores/auth';

const defaultAvatar = '/default-avatar.png';

function getAuthHeaders(): Record<string, string> {
  const auth = useAuthStore();
  return {
    'X-Request-Id': crypto.randomUUID(),
    'X-Tenant-Id': auth.tenantId,
    'Authorization': `Bearer ${auth.accessToken}`
  };
}

interface Member {
  id: number;
  userId: number;
  nickname: string;
  avatarUrl: string;
  mobile: string;
  phone: string;
  email: string;
  roleCode: string;
  roleName: string;
  status: string;
  createdBy: number;
  creatorName: string;
  createdAt: string;
}

interface Role {
  id: number;
  roleCode: string;
  roleName: string;
  description: string;
  permissions: string[];
  sortOrder: number;
  isSystem: boolean;
}

const members = ref<Member[]>([]);
const roles = ref<Role[]>([]);
const loading = ref(false);
const inviting = ref(false);
const savingRole = ref(false);
const showInviteDialog = ref(false);
const showRoleDialog = ref(false);
const currentMember = ref<Member | null>(null);

const filters = reactive({
  keyword: '',
  status: '',
  roleCode: ''
});

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
});

const inviteForm = reactive({
  contact: '',
  roleCode: '',
  remark: ''
});

const roleForm = reactive({
  roleCode: ''
});

const inviteRules: FormRules = {
  contact: [
    { required: true, message: '请输入手机号或邮箱', trigger: 'blur' },
    {
      pattern: /^(1[3-9]\d{9}|[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})$/,
      message: '请输入正确的手机号或邮箱',
      trigger: 'blur'
    }
  ],
  roleCode: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
};

const roleRules: FormRules = {
  roleCode: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
};

const inviteFormRef = ref<FormInstance | null>(null);
const roleFormRef = ref<FormInstance | null>(null);

onMounted(() => {
  loadRoles();
  loadMembers();
});

async function loadRoles() {
  try {
    const response = await fetch('/api/tenant/roles', {
      headers: getAuthHeaders()
    });
    const payload = await response.json();
    if (payload.code === 0) {
      roles.value = payload.data;
    }
  } catch (error) {
    ElMessage.error('加载角色列表失败');
  }
}

async function loadMembers() {
  loading.value = true;
  try {
    const params = new URLSearchParams({
      page: String(pagination.page),
      pageSize: String(pagination.pageSize),
      keyword: filters.keyword,
      status: filters.status,
      roleCode: filters.roleCode
    });

    const response = await fetch(`/api/tenant/members/v2?${params}`, {
      headers: getAuthHeaders()
    });
    const payload = await response.json();
    if (payload.code === 0) {
      members.value = payload.data.members;
      pagination.total = payload.data.total;
    } else {
      ElMessage.error(payload.message || '加载成员列表失败');
    }
  } catch (error) {
    ElMessage.error('加载成员列表失败');
  } finally {
    loading.value = false;
  }
}

async function handleInvite() {
  if (!inviteFormRef.value) return;
  await inviteFormRef.value.validate();

  inviting.value = true;
  try {
    const response = await fetch('/api/tenant/members', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeaders()
      },
      body: JSON.stringify(inviteForm)
    });
    const payload = await response.json();
    if (payload.code === 0) {
      ElMessage.success('添加成功');
      showInviteDialog.value = false;
      inviteForm.contact = '';
      inviteForm.roleCode = '';
      inviteForm.remark = '';
      await loadMembers();
    } else {
      ElMessage.error(payload.message || '添加失败');
    }
  } catch (error) {
    ElMessage.error('添加失败');
  } finally {
    inviting.value = false;
  }
}

function handleEditRole(member: Member) {
  currentMember.value = member;
  roleForm.roleCode = member.roleCode;
  showRoleDialog.value = true;
}

async function handleSaveRole() {
  if (!roleFormRef.value || !currentMember.value) return;
  await roleFormRef.value.validate();

  savingRole.value = true;
  try {
    const response = await fetch(`/api/tenant/members/${currentMember.value.userId}/role`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeaders()
      },
      body: JSON.stringify(roleForm)
    });
    const payload = await response.json();
    if (payload.code === 0) {
      ElMessage.success('修改成功');
      showRoleDialog.value = false;
      await loadMembers();
    } else {
      ElMessage.error(payload.message || '修改失败');
    }
  } catch (error) {
    ElMessage.error('修改失败');
  } finally {
    savingRole.value = false;
  }
}

async function handleToggleStatus(member: Member) {
  const newStatus = member.status === 'active' ? 'inactive' : 'active';
  const action = newStatus === 'active' ? '启用' : '禁用';

  try {
    await ElMessageBox.confirm(`确定要${action}该成员吗？`, '确认', {
      type: 'warning'
    });

    const response = await fetch(`/api/tenant/members/${member.userId}/status`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeaders()
      },
      body: JSON.stringify({ status: newStatus })
    });
    const payload = await response.json();
    if (payload.code === 0) {
      ElMessage.success(`${action}成功`);
      await loadMembers();
    } else {
      ElMessage.error(payload.message || `${action}失败`);
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`);
    }
  }
}

async function handleRemove(member: Member) {
  try {
    await ElMessageBox.confirm('确定要移除该成员吗？此操作不可恢复。', '确认移除', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    });

    const response = await fetch(`/api/tenant/members/${member.userId}`, {
      method: 'DELETE',
      headers: getAuthHeaders()
    });
    const payload = await response.json();
    if (payload.code === 0) {
      ElMessage.success('移除成功');
      await loadMembers();
    } else {
      ElMessage.error(payload.message || '移除失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('移除失败');
    }
  }
}

function getRoleTagType(roleCode: string): string {
  const tagTypes: Record<string, string> = {
    'boss': 'danger',
    'tenant_admin': 'warning',
    'tenant_operator': 'primary',
    'tenant_viewer': 'info',
    'designer': 'primary',
    'design_assistant': 'success',
    'pattern_maker': 'warning',
    'operator': 'info',
    'viewer': 'info'
  };
  return tagTypes[roleCode] || '';
}

function getRoleCode(row: Member): string {
  // Handle different data structures from API
  if (row.roleCode) return row.roleCode;
  if (row.role) return row.role;
  return '';
}

function getStatus(row: Member): string {
  return row.status || 'active';
}
</script>

<style scoped>
.members-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  background: #ffffff;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(18, 30, 61, 0.08);
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.name {
  font-weight: 500;
  color: #121e3d;
  margin-bottom: 4px;
}

.contact {
  font-size: 13px;
  color: #5a677f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>