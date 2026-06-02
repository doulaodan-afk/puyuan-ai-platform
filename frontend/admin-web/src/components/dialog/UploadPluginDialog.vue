<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal">
      <div class="modal-header">
        <h2>上传插件</h2>
        <button @click="close" class="close-btn">
          <el-icon><Close /></el-icon>
        </button>
      </div>
      <div class="modal-body">
        <div class="upload-zone" :class="{ dragging: dragging }" @dragover.prevent="dragging = true"
          @dragleave="dragging = false" @drop.prevent="onDrop">
          <input type="file" accept=".zip" id="plugin-zip-input" @change="onFileSelected" />
          <div v-if="!selectedFile" class="upload-placeholder">
            <el-icon size="32"><Upload /></el-icon>
            <p>点击选择或拖拽 .zip 插件包</p>
            <p class="hint">最大 50MB</p>
          </div>
          <div v-else class="file-info">
            <el-icon size="20"><Document /></el-icon>
            <span>{{ selectedFile.name }}</span>
            <button class="clear-btn" @click="clearFile">×</button>
          </div>
        </div>

        <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>

        <div class="override-row" v-if="showOverride">
          <label class="check-label">
            <input type="checkbox" v-model="overrideExisting" />
            覆盖已有插件（plugin_id 重复时）
          </label>
        </div>
      </div>
      <div class="modal-footer">
        <button @click="close" :disabled="uploading">取消</button>
        <button @click="submitUpload" :disabled="!selectedFile || uploading" class="btn-primary">
          {{ uploading ? '上传中...' : '上传' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { Close, Upload, Document } from "@element-plus/icons-vue";
import { uploadPlugin } from "../../api/plugin";

const props = defineProps<{ visible: boolean }>();
const emit = defineEmits<{
  (e: "update:visible", v: boolean): void;
  (e: "success"): void;
}>();

const dragging = ref(false);
const selectedFile = ref<File | null>(null);
const overrideExisting = ref(false);
const uploading = ref(false);
const errorMessage = ref("");
const showOverride = ref(false);

watch(() => props.visible, (v) => {
  if (!v) {
    selectedFile.value = null;
    overrideExisting.value = false;
    uploading.value = false;
    errorMessage.value = "";
    showOverride.value = false;
  }
});

function close() {
  emit("update:visible", false);
}

function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement;
  if (input.files && input.files[0]) {
    selectedFile.value = input.files[0];
    errorMessage.value = "";
  }
}

function onDrop(e: DragEvent) {
  dragging.value = false;
  const file = e.dataTransfer?.files?.[0];
  if (file) {
    selectedFile.value = file;
    errorMessage.value = "";
  }
}

function clearFile() {
  selectedFile.value = null;
  const input = document.getElementById("plugin-zip-input") as HTMLInputElement;
  if (input) input.value = "";
}

async function submitUpload() {
  if (!selectedFile.value) return;
  uploading.value = true;
  errorMessage.value = "";
  try {
    await uploadPlugin(selectedFile.value, overrideExisting.value);
    close();
    emit("success");
  } catch (err) {
    const msg = err instanceof Error ? err.message : "上传失败";
    if (msg.includes("已存在")) {
      showOverride.value = true;
    }
    errorMessage.value = msg;
  } finally {
    uploading.value = false;
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
  max-width: 480px;
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
  gap: 12px;
}

.upload-zone {
  border: 2px dashed hsl(var(--border));
  border-radius: var(--radius);
  padding: 32px 20px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s;
  position: relative;
}

.upload-zone.dragging {
  border-color: hsl(var(--primary));
}

.upload-zone input[type="file"] {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
  width: 100%;
  height: 100%;
}

.upload-placeholder {
  color: hsl(var(--muted-foreground));
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-placeholder p {
  margin: 0;
  font-size: 14px;
}

.upload-placeholder .hint {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: hsl(var(--foreground));
  justify-content: center;
}

.file-info span {
  word-break: break-all;
}

.clear-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: hsl(var(--muted-foreground));
  font-size: 18px;
  padding: 0 4px;
}

.clear-btn:hover {
  color: hsl(var(--foreground));
}

.error-msg {
  color: hsl(var(--destructive));
  font-size: 13px;
  padding: 8px 12px;
  background: hsl(var(--destructive) / 0.1);
  border-radius: calc(var(--radius) - 4px);
  border: 1px solid hsl(var(--destructive) / 0.3);
}

.override-row {
  display: flex;
  align-items: center;
}

.check-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: hsl(var(--foreground));
  cursor: pointer;
}

.check-label input[type="checkbox"] {
  cursor: pointer;
  accent-color: hsl(var(--primary));
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid hsl(var(--border));
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.modal-footer button {
  min-width: 80px;
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 8px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
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

.modal-footer button:first-child {
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
}
</style>