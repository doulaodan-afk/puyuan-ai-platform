<template>
  <section class="page-container">
    <header class="row-head">
      <h1>场景模型配置</h1>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-if="successMessage" class="success">{{ successMessage }}</p>

    <!-- 场景列表 -->
    <div v-if="!loading" class="scenes-grid">
      <div v-for="scene in scenes" :key="scene.scene_code" class="scene-card">
        <div class="scene-header">
          <div>
            <h3>{{ scene.scene_name }}</h3>
            <code class="scene-code">{{ scene.scene_code }}</code>
            <span class="api-type-tag">{{ scene.api_type }}</span>
          </div>
          <div class="scene-header-actions">
            <button class="btn-sm btn-recommend" @click="openRecommend(scene)" :disabled="recommending === scene.scene_code">
              {{ recommending === scene.scene_code ? '推荐中...' : '🤖 AI 推荐' }}
            </button>
            <button class="btn-sm" @click="openBind(scene)">+ 绑定模型</button>
          </div>
        </div>
        <p class="scene-desc">{{ scene.scene_description }}</p>

        <!-- 已绑定的模型 -->
        <div v-if="scene.models.length > 0" class="bindings-list">
          <div v-for="m in scene.models" :key="m.id" class="binding-item" :class="{ 'is-primary': m.is_primary, 'is-fallback': m.is_fallback }">
            <div class="binding-info">
              <span class="model-id">{{ m.model_id }}</span>
              <span class="provider-tag">{{ m.provider_display_name || m.provider_name }}</span>
              <span v-if="m.is_primary" class="tag tag-primary">主模型</span>
              <span v-if="m.is_fallback" class="tag tag-fallback">备用</span>
              <span class="priority-tag">优先级: {{ m.priority }}</span>
            </div>
            <div class="binding-actions">
              <button v-if="!m.is_primary" class="btn-xs" @click="setPrimaryModel(scene.scene_code, m.id)">设为主模型</button>
              <button v-if="!m.is_fallback" class="btn-xs" @click="setFallbackModel(scene.scene_code, m.id)">设为备用</button>
              <button class="btn-xs btn-danger" @click="unbindHandler(scene.scene_code, m)">解除</button>
            </div>
          </div>
        </div>
        <p v-else class="no-bindings">暂未绑定模型</p>
      </div>
    </div>
    <p v-else class="loading">加载中...</p>

    <!-- 绑定模型对话框 -->
    <div v-if="showBindDialog" class="modal-overlay" @click="closeBindDialog">
      <div class="modal modal-wide" @click.stop>
        <h3>为「{{ bindTarget?.scene_name }}」绑定模型</h3>
        <form class="bind-form" @submit.prevent="doBind">
          <div class="form-field">
            <label>选择提供商</label>
            <select v-model.number="bindForm.providerId" required @change="providerModels = []; downloadError = ''; showModelDropdown = false">
              <option :value="0" disabled>-- 选择提供商 --</option>
              <option v-for="p in providers" :key="p.id" :value="p.id">{{ p.display_name }} ({{ p.name }})</option>
            </select>
          </div>
          <div class="form-field">
            <label>模型 ID</label>
            <div class="model-id-row">
              <div class="model-id-input-wrapper">
                <input v-model.trim="bindForm.modelId" placeholder="如 deepseek-ai/DeepSeek-V3" required />
                <!-- 模型下拉列表 -->
                <div v-if="showModelDropdown && providerModels.length > 0" class="model-dropdown">
                  <div
                    v-for="m in providerModels"
                    :key="m.model_id"
                    class="model-dropdown-item"
                    :class="{ selected: bindForm.modelId === m.model_id }"
                    @click="selectModel(m.model_id)"
                  >
                    <span class="model-dropdown-id">{{ m.model_id }}</span>
                  </div>
                </div>
              </div>
              <button type="button" class="btn-download" @click="downloadModels" :disabled="downloadingModels || !bindForm.providerId">
                {{ downloadingModels ? '下载中...' : '下载模型' }}
              </button>
            </div>
            <p v-if="downloadError" class="field-error">{{ downloadError }}</p>
          </div>
          <div class="form-field">
            <label>优先级</label>
            <input v-model.number="bindForm.priority" type="number" min="0" max="99" />
          </div>
          <div class="form-checks">
            <label class="checkbox"><input v-model="bindForm.isPrimary" type="checkbox" /> 设为主模型</label>
            <label class="checkbox"><input v-model="bindForm.isFallback" type="checkbox" /> 设为备用模型</label>
          </div>
          <div class="form-actions">
            <button type="submit" :disabled="bindSaving">{{ bindSaving ? '保存中...' : '绑定' }}</button>
            <button type="button" @click="closeBindDialog">取消</button>
          </div>
        </form>
      </div>
    </div>

    <!-- AI 推荐对话框 -->
    <div v-if="showRecommendDialog" class="modal-overlay" @click="closeRecommendDialog">
      <div class="modal modal-wide" @click.stop>
        <h3>AI 推荐 — {{ recommendTarget?.scene_name }}</h3>
        <p class="hint">AI 将分析可用模型并推荐最适合该场景的 3 个模型</p>

        <div v-if="recommendLoading" class="loading-inline">正在分析可用模型...</div>

        <div v-else-if="recommendError" class="error-inline">{{ recommendError }}</div>

        <div v-else-if="recommendResults.length > 0" class="recommend-list">
          <div v-for="(m, idx) in recommendResults" :key="idx" class="recommend-item">
            <div class="recommend-rank">#{{ idx + 1 }}</div>
            <div class="recommend-info">
              <div class="recommend-model">{{ m.model_id }}</div>
              <div class="recommend-provider">{{ m.provider_name }}</div>
              <div class="recommend-reason">{{ m.reason }}</div>
            </div>
            <label class="checkbox adopt-check">
              <input v-model="recommendSelected[idx]" type="checkbox" />
              采纳
            </label>
          </div>
          <div class="form-actions">
            <button @click="adoptRecommendations" :disabled="!hasSelection">采纳选中模型</button>
            <button type="button" @click="closeRecommendDialog">关闭</button>
          </div>
        </div>
        <p v-else class="empty-inline">未找到推荐模型</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import {
  type SceneOverview,
  type SceneModelBinding,
  type AiProvider,
  type RecommendedModel,
  type ProviderModelEntry,
  listScenes,
  listProviders,
  listProviderModels,
  bindModel,
  unbindModel,
  setPrimary,
  setFallback,
  recommendModels,
} from "../api/aiScene";

const scenes = ref<SceneOverview[]>([]);
const providers = ref<AiProvider[]>([]);
const loading = ref(true);
const errorMessage = ref("");
const successMessage = ref("");

// 绑定对话框
const showBindDialog = ref(false);
const bindTarget = ref<SceneOverview | null>(null);
const bindSaving = ref(false);
const bindForm = reactive({ providerId: 0, modelId: "", priority: 0, isPrimary: false, isFallback: false });

// 模型下载
const providerModels = ref<ProviderModelEntry[]>([]);
const downloadingModels = ref(false);
const downloadError = ref("");
const showModelDropdown = ref(false);

// 推荐对话框
const showRecommendDialog = ref(false);
const recommendTarget = ref<SceneOverview | null>(null);
const recommendResults = ref<RecommendedModel[]>([]);
const recommendSelected = ref<boolean[]>([]);
const recommendLoading = ref(false);
const recommendError = ref("");
const recommending = ref("");

const hasSelection = computed(() => {
  return recommendSelected.value.some(v => v === true);
});

function clearMessages() {
  errorMessage.value = "";
  successMessage.value = "";
}

async function loadData() {
  loading.value = true;
  clearMessages();
  try {
    const [s, p] = await Promise.all([listScenes(), listProviders()]);
    scenes.value = s;
    providers.value = p;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

// ========== 绑定模型 ==========

function openBind(scene: SceneOverview) {
  bindTarget.value = scene;
  bindForm.providerId = providers.value.length > 0 ? providers.value[0].id : 0;
  bindForm.modelId = "";
  bindForm.priority = 0;
  bindForm.isPrimary = false;
  bindForm.isFallback = false;
  showBindDialog.value = true;
}

function closeBindDialog() {
  showBindDialog.value = false;
  bindTarget.value = null;
  providerModels.value = [];
  downloadError.value = "";
  showModelDropdown.value = false;
}

async function doBind() {
  if (!bindTarget.value || !bindForm.providerId || !bindForm.modelId) return;
  bindSaving.value = true;
  clearMessages();
  try {
    await bindModel(bindTarget.value.scene_code, {
      provider_id: bindForm.providerId,
      model_id: bindForm.modelId,
      priority: bindForm.priority,
      is_primary: bindForm.isPrimary,
      is_fallback: bindForm.isFallback,
    });
    successMessage.value = `已为「${bindTarget.value.scene_name}」绑定模型 ${bindForm.modelId}`;
    closeBindDialog();
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "绑定失败";
  } finally {
    bindSaving.value = false;
  }
}

async function downloadModels() {
  if (!bindForm.providerId) {
    downloadError.value = "请先选择提供商";
    return;
  }
  downloadingModels.value = true;
  downloadError.value = "";
  providerModels.value = [];
  try {
    const models = await listProviderModels(bindForm.providerId);
    providerModels.value = models;
    showModelDropdown.value = true;
    if (models.length === 0) {
      downloadError.value = "该提供商暂无可用模型";
    }
  } catch (error) {
    downloadError.value = error instanceof Error ? error.message : "获取模型列表失败";
  } finally {
    downloadingModels.value = false;
  }
}

function selectModel(modelId: string) {
  bindForm.modelId = modelId;
  showModelDropdown.value = false;
}

// ========== 主/备用模型设置 ==========

async function setPrimaryModel(sceneCode: string, bindingId: number) {
  clearMessages();
  try {
    await setPrimary(sceneCode, bindingId);
    successMessage.value = `已设置主模型`;
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "设置失败";
  }
}

async function setFallbackModel(sceneCode: string, bindingId: number) {
  clearMessages();
  try {
    await setFallback(sceneCode, bindingId);
    successMessage.value = `已设置备用模型`;
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "设置失败";
  }
}

async function unbindHandler(sceneCode: string, m: SceneModelBinding) {
  if (!confirm(`确定解除「${sceneCode}」场景下模型 "${m.model_id}" 的绑定吗？`)) return;
  clearMessages();
  try {
    await unbindModel(m.id);
    successMessage.value = `已解除绑定`;
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "解除失败";
  }
}

// ========== AI 推荐 ==========

function openRecommend(scene: SceneOverview) {
  recommendTarget.value = scene;
  recommendResults.value = [];
  recommendSelected.value = [];
  recommendLoading.value = true;
  recommendError.value = "";
  recommending.value = scene.scene_code;
  showRecommendDialog.value = true;

  recommendModels(scene.scene_code)
    .then((resp) => {
      recommendResults.value = resp.recommended_models;
      recommendSelected.value = resp.recommended_models.map(() => false);
    })
    .catch((error) => {
      recommendError.value = error instanceof Error ? error.message : "推荐失败";
    })
    .finally(() => {
      recommendLoading.value = false;
      recommending.value = "";
    });
}

function closeRecommendDialog() {
  showRecommendDialog.value = false;
  recommendTarget.value = null;
}

async function adoptRecommendations() {
  if (!recommendTarget.value) return;
  const scene = recommendTarget.value;
  const selected = recommendResults.value.filter((_, i) => recommendSelected.value[i]);

  if (selected.length === 0) return;

  clearMessages();
  let successCount = 0;
  for (let i = 0; i < selected.length; i++) {
    const m = selected[i];
    try {
      await bindModel(scene.scene_code, {
        provider_id: m.provider_id,
        model_id: m.model_id,
        is_primary: i === 0,    // 第一个设为主模型
        is_fallback: i === 1,    // 第二个设为备用
        priority: i,
      });
      successCount++;
    } catch (error) {
      console.error("Failed to adopt model:", m.model_id, error);
    }
  }

  successMessage.value = `成功采纳 ${successCount}/${selected.length} 个推荐模型`;
  closeRecommendDialog();
  await loadData();
}

onMounted(() => {
  void loadData();
});
</script>

<style scoped>
.page-container { max-width: 1200px; margin: 0 auto; padding: 16px; }

.row-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.row-head h1 { font-size: 24px; font-weight: 600; color: hsl(var(--foreground)); }

.error { background: hsl(var(--destructive) / 0.1); color: hsl(var(--destructive)); padding: 12px 16px; border-radius: 8px; margin-bottom: 16px; border: 1px solid hsl(var(--destructive) / 0.2); }
.success { background: hsl(142 71% 45% / 0.1); color: hsl(142 71% 45%); padding: 12px 16px; border-radius: 8px; margin-bottom: 16px; border: 1px solid hsl(142 71% 45% / 0.2); font-weight: 500; }
.loading { text-align: center; padding: 40px 0; color: hsl(var(--muted-foreground)); }

.scenes-grid { display: grid; grid-template-columns: 1fr; gap: 16px; }
@media (min-width: 768px) { .scenes-grid { grid-template-columns: repeat(2, 1fr); } }

.scene-card { background: hsl(var(--card)); border: 1px solid hsl(var(--border)); border-radius: 8px; padding: 16px; }
.scene-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; gap: 8px; }
.scene-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: hsl(var(--foreground)); }
.scene-code { font-size: 12px; color: hsl(var(--muted-foreground)); background: hsl(var(--muted)); padding: 2px 6px; border-radius: 4px; margin-left: 8px; }
.api-type-tag { font-size: 11px; color: hsl(var(--primary)); background: hsl(var(--primary) / 0.1); padding: 2px 8px; border-radius: 10px; margin-left: 8px; }
.scene-header-actions { display: flex; gap: 6px; flex-shrink: 0; }
.scene-desc { font-size: 13px; color: hsl(var(--muted-foreground)); margin-bottom: 12px; }

.bindings-list { display: flex; flex-direction: column; gap: 6px; }
.binding-item { background: hsl(var(--muted)); padding: 10px 12px; border-radius: 6px; display: flex; justify-content: space-between; align-items: center; gap: 8px; flex-wrap: wrap; border: 1px solid transparent; }
.binding-item.is-primary { border-color: hsl(var(--primary)); background: hsl(var(--primary) / 0.05); }
.binding-item.is-fallback { border-color: hsl(45 93% 47%); background: hsl(45 93% 47% / 0.05); }
.binding-info { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.model-id { font-family: "Consolas", monospace; font-size: 13px; font-weight: 500; color: hsl(var(--foreground)); }
.provider-tag { font-size: 11px; background: hsl(var(--secondary)); color: hsl(var(--secondary-foreground)); padding: 2px 6px; border-radius: 4px; }
.tag { font-size: 10px; padding: 1px 6px; border-radius: 8px; font-weight: 600; }
.tag-primary { background: hsl(var(--primary)); color: hsl(var(--primary-foreground)); }
.tag-fallback { background: hsl(45 93% 47%); color: #000; }
.priority-tag { font-size: 11px; color: hsl(var(--muted-foreground)); }
.binding-actions { display: flex; gap: 4px; }
.no-bindings { font-size: 13px; color: hsl(var(--muted-foreground)); font-style: italic; }

button { border: none; border-radius: 6px; padding: 8px 16px; background: hsl(var(--primary)); color: hsl(var(--primary-foreground)); font-size: 14px; font-weight: 500; cursor: pointer; }
button:hover:not(:disabled) { opacity: 0.9; }
button:disabled { opacity: 0.5; cursor: not-allowed; }
button[type="button"] { background: hsl(var(--secondary)); color: hsl(var(--secondary-foreground)); }
.btn-sm { padding: 5px 12px; font-size: 12px; }
.btn-xs { padding: 3px 8px; font-size: 11px; }
.btn-danger { background: hsl(var(--destructive)) !important; color: hsl(var(--destructive-foreground)) !important; }
.btn-recommend { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important; color: white !important; }

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: hsl(0 0% 0% / 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: hsl(var(--card)); padding: 24px; border-radius: 8px; max-width: 440px; width: 90%; border: 1px solid hsl(var(--border)); max-height: 80vh; overflow-y: auto; }
.modal-wide { max-width: 580px; }
.modal h3 { margin-top: 0; margin-bottom: 16px; font-size: 18px; font-weight: 600; }

.bind-form { display: flex; flex-direction: column; gap: 12px; }
.form-field { display: flex; flex-direction: column; gap: 4px; }
.form-field label { font-size: 13px; font-weight: 500; color: hsl(var(--muted-foreground)); }
.form-field input, .form-field select { padding: 10px 14px; border: 1px solid hsl(var(--border)); border-radius: 6px; font-size: 14px; color: hsl(var(--foreground)); background: hsl(var(--background)); }
.form-checks { display: flex; gap: 16px; }
.checkbox { display: flex; align-items: center; gap: 6px; font-size: 13px; color: hsl(var(--foreground)); }
.form-actions { display: flex; gap: 8px; margin-top: 8px; }

.hint { font-size: 13px; color: hsl(var(--muted-foreground)); margin-bottom: 12px; }
.loading-inline { text-align: center; padding: 20px; color: hsl(var(--muted-foreground)); }
.error-inline { color: hsl(var(--destructive)); padding: 12px; }
.empty-inline { text-align: center; padding: 20px; color: hsl(var(--muted-foreground)); }

.recommend-list { display: flex; flex-direction: column; gap: 10px; }
.recommend-item { display: flex; gap: 12px; align-items: flex-start; padding: 12px; background: hsl(var(--muted)); border-radius: 8px; }
.recommend-rank { font-size: 20px; font-weight: 700; color: hsl(var(--primary)); min-width: 32px; }
.recommend-info { flex: 1; }
.recommend-model { font-family: "Consolas", monospace; font-size: 14px; font-weight: 600; color: hsl(var(--foreground)); }
.recommend-provider { font-size: 12px; color: hsl(var(--muted-foreground)); margin-top: 2px; }
.recommend-reason { font-size: 12px; color: hsl(var(--muted-foreground)); margin-top: 4px; line-height: 1.4; }
.adopt-check { align-self: center; white-space: nowrap; }

/* Model download */
.model-id-row { display: flex; gap: 8px; align-items: flex-start; }
.model-id-input-wrapper { position: relative; flex: 1; }
.model-id-input-wrapper input { width: 100%; padding: 10px 14px; border: 1px solid hsl(var(--border)); border-radius: 6px; font-size: 14px; color: hsl(var(--foreground)); background: hsl(var(--background)); box-sizing: border-box; }
.btn-download { padding: 10px 14px; font-size: 13px; white-space: nowrap; background: hsl(var(--secondary)) !important; color: hsl(var(--secondary-foreground)) !important; }
.btn-download:hover:not(:disabled) { opacity: 0.85; }
.field-error { font-size: 12px; color: hsl(var(--destructive)); margin-top: 4px; }

.model-dropdown { position: absolute; top: 100%; left: 0; right: 0; margin-top: 2px; max-height: 200px; overflow-y: auto; background: hsl(var(--card)); border: 1px solid hsl(var(--border)); border-radius: 6px; box-shadow: 0 6px 16px rgba(0,0,0,0.1); z-index: 50; }
.model-dropdown-item { padding: 8px 14px; font-family: "Consolas", monospace; font-size: 13px; color: hsl(var(--foreground)); cursor: pointer; transition: background 0.1s ease; }
.model-dropdown-item:hover { background: hsl(var(--accent)); }
.model-dropdown-item.selected { background: hsl(var(--primary) / 0.1); color: hsl(var(--primary)); }
.model-dropdown-id { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
</style>
