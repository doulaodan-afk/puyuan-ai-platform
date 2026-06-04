<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal" style="max-width: 560px;">
      <div class="modal-header">
        <h2>AI 模型配置</h2>
        <button @click="close" class="close-btn">
          <el-icon><Close /></el-icon>
        </button>
      </div>
      <div class="modal-body">
        <div class="info-row">
          <span class="info-label">插件：</span>
          <span class="info-value">{{ pluginId }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">当前模型：</span>
          <span class="info-value">{{ currentModel || '默认' }}</span>
        </div>

        <div class="select-section">
          <label class="select-label">选择 AI 模型</label>
          <el-select
            v-model="selectedModel"
            placeholder="选择模型..."
            :loading="modelsLoading"
            filterable
            class="model-select"
          >
            <el-option label="使用默认模型" value="" />
            <el-option
              v-for="model in models"
              :key="model.id"
              :label="model.id + (model.owned_by ? ' (' + model.owned_by + ')' : '')"
              :value="model.id"
            />
          </el-select>
          <p v-if="modelsLoading" class="hint">正在加载模型列表...</p>
          <p v-else-if="models.length === 0" class="hint">暂无可用模型，请检查 AI 服务配置</p>
        </div>

        <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>
      </div>
      <div class="modal-footer">
        <button @click="close" :disabled="submitting">取消</button>
        <button @click="confirmSave" :disabled="submitting" class="btn-primary">
          {{ submitting ? "保存中..." : "保存" }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { Close } from "@element-plus/icons-vue";
import { getAiModels, updatePluginModel, type AiModelItem } from "../../api/plugin";

const props = defineProps<{
  visible: boolean;
  pluginId: string;
  currentModel?: string;
}>();
const emit = defineEmits<{
  (e: "update:visible", v: boolean): void;
  (e: "success"): void;
}>();

const models = ref<AiModelItem[]>([]);
const modelsLoading = ref(false);
const selectedModel = ref("");
const submitting = ref(false);
const errorMessage = ref("");

watch(() => props.visible, async (v) => {
  if (v) {
    errorMessage.value = "";
    selectedModel.value = props.currentModel || "";
    await loadModels();
  }
});

async function loadModels() {
  modelsLoading.value = true;
  try {
    models.value = await getAiModels();
  } catch {
    models.value = [];
    errorMessage.value = "加载模型列表失败";
  } finally {
    modelsLoading.value = false;
  }
}

function close() {
  emit("update:visible", false);
}

async function confirmSave() {
  if (!props.pluginId) return;
  submitting.value = true;
  errorMessage.value = "";
  try {
    await updatePluginModel(props.pluginId, selectedModel.value);
    close();
    emit("success");
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : "保存失败";
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: hsl(0 0% 0% / 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal {
  background: hsl(var(--card));
  border-radius: var(--radius);
  max-width: 560px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  border: 1px solid hsl(var(--border));
  box-shadow: var(--shadow-lg);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid hsl(var(--border));
}

.modal-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.close-btn {
  background: none;
  border: none;
  padding: 4px;
  color: hsl(var(--muted-foreground));
  cursor: pointer;
  display: flex;
  align-items: center;
  border-radius: calc(var(--radius) - 6px);
  font-size: 18px;
}

.close-btn:hover {
  background: hsl(var(--accent));
  color: hsl(var(--foreground));
}

.modal-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-label {
  font-size: 14px;
  color: hsl(var(--muted-foreground));
  min-width: 80px;
}

.info-value {
  font-size: 14px;
  color: hsl(var(--foreground));
  font-weight: 500;
}

.select-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.select-label {
  font-size: 14px;
  color: hsl(var(--foreground));
  font-weight: 500;
}

.model-select {
  width: 100%;
}

.hint {
  font-size: 13px;
  color: hsl(var(--muted-foreground));
  margin: 0;
}

.error-msg {
  color: hsl(var(--destructive));
  font-size: 13px;
  padding: 8px 12px;
  background: hsl(var(--destructive) / 0.1);
  border-radius: calc(var(--radius) - 4px);
  border: 1px solid hsl(var(--destructive) / 0.3);
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid hsl(var(--border));
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.modal-footer button {
  min-width: 100px;
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 8px 16px;
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.modal-footer button:hover:not(:disabled) {
  opacity: 0.9;
}

.modal-footer button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-footer .btn-primary {
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
}
</style>