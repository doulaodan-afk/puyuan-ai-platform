<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  createRequirement,
  chat,
  summarize,
  confirmRequirement,
  transferToAssistant,
  getBalance,
} from '../api'
import { useDesignAssistantStore } from '../stores'
import { Promotion, Microphone } from '@element-plus/icons-vue'

const router = useRouter()
const store = useDesignAssistantStore()

// UI 状态
const isRecording = ref(false)
const interimTranscript = ref('')
const userInput = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

// 余额
const balance = ref<number>(0)

// 面料商选择
const availableSuppliers = ref<SupplierInfo[]>([])
const selectedSupplierId = ref<number | null>(null)

// 当前状态：chat（对话中）/ summary（总结预览）
const currentView = ref('chat')

// 是否已初始化会话
const sessionInitialized = ref(false)

// 语音识别
let recognition: any = null

onMounted(async () => {
  initSpeechRecognition()

  // 加载余额
  try {
    const data = await getBalance()
    balance.value = data.balance
  } catch (e) {
    console.error('获取余额失败', e)
  }

  // 加载可合作的供应商
  loadSuppliers()

  // 滚动到底部
  scrollToBottom()
})

onUnmounted(() => {
  if (isRecording.value && recognition) {
    recognition.stop()
  }
})

function initSpeechRecognition() {
  if ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window) {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
    recognition = new SpeechRecognition()
    recognition.lang = 'zh-CN'
    recognition.interimResults = true
    recognition.continuous = false

    recognition.onresult = (event: any) => {
      let interim = ''
      let final = ''

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript
        if (event.results[i].isFinal) {
          final += transcript
        } else {
          interim += transcript
        }
      }

      interimTranscript.value = interim
      if (final) {
        userInput.value += final
      }
    }

    recognition.onerror = (event: any) => {
      console.error('语音识别错误:', event.error)
      isRecording.value = false
    }

    recognition.onend = () => {
      if (isRecording.value) {
        setTimeout(() => {
          if (isRecording.value) {
            recognition.start()
          }
        }, 100)
      }
    }
  }
}

function toggleRecording() {
  if (!recognition) {
    alert('您的浏览器不支持语音识别')
    return
  }

  if (isRecording.value) {
    recognition.stop()
    isRecording.value = false
    interimTranscript.value = ''
  } else {
    userInput.value = ''
    recognition.start()
    isRecording.value = true
  }
}

// 图片上传
async function handleImageUpload(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return
  const names: string[] = []
  for (const file of input.files) {
    const url = URL.createObjectURL(file)
    store.addImage(url)
    names.push(file.name)
  }
  await sendSystemMessage(`已上传参考图片：${names.join('、')}`)
  input.value = ''
}

// 视频上传
async function handleVideoUpload(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return
  const names: string[] = []
  for (const file of input.files) {
    const url = URL.createObjectURL(file)
    store.addVideo(url)
    names.push(file.name)
  }
  await sendSystemMessage(`已上传参考视频：${names.join('、')}`)
  input.value = ''
}

// 发送系统消息
async function sendSystemMessage(content: string) {
  store.addMessage({
    role: 'user',
    content,
    time: new Date().toISOString(),
  })

  await processChatMessage(content)
}

// 发送消息
async function sendMessage() {
  const message = userInput.value.trim()
  if (!message) return

  userInput.value = ''

  store.addMessage({
    role: 'user',
    content: message,
    time: new Date().toISOString(),
  })

  await processChatMessage(message)
}

// 处理聊天消息
async function processChatMessage(message: string) {
  store.isChatting = true

  // 首次对话时创建需求
  if (!sessionInitialized.value) {
    await initializeSession(message)
    return
  }

  // 继续对话
  try {
    const response = await chat({
      sessionId: store.currentSessionId || undefined,
      message,
      requirementId: store.currentRequirementId || undefined,
    })

    store.setSessionId(response.sessionId)
    store.setConversation(response.conversationHistory)

    await nextTick(() => scrollToBottom())
  } catch (e: any) {
    console.error('对话失败:', e)
  } finally {
    store.isChatting = false
  }
}

// 初始化会话
async function initializeSession(firstMessage: string) {
  try {
    // 创建需求记录
    const req = await createRequirement({
      title: '设计需求 - ' + new Date().toLocaleDateString(),
      rawImages: store.uploadedImages,
      rawVideos: store.uploadedVideos,
      rawText: firstMessage,
      conversationHistory: store.conversationHistory,
      selectedSupplierId: selectedSupplierId.value ?? undefined,
    })

    store.currentRequirementId = req.id
    sessionInitialized.value = true

    // 开始对话
    const response = await chat({
      message: firstMessage,
      requirementId: req.id,
    })

    store.setSessionId(response.sessionId)
    store.setConversation(response.conversationHistory)

    await nextTick(() => scrollToBottom())
  } catch (e: any) {
    console.error('初始化会话失败:', e)
    alert('启动对话失败: ' + e.message)
    sessionInitialized.value = false
  } finally {
    store.isChatting = false
  }
}

// 生成 AI 总结
async function generateSummary() {
  if (!store.currentRequirementId) {
    alert('请先开始对话')
    return
  }

  store.isSummarizing = true

  try {
    const response = await summarize(store.currentRequirementId)
    store.setAiSummary(response.summaryData, response.aiSummary)
    currentView.value = 'summary'
  } catch (e: any) {
    alert('生成总结失败: ' + e.message)
  } finally {
    store.isSummarizing = false
  }
}

// 确认发布
async function confirmAndPublish() {
  if (!store.currentRequirementId) return

  try {
    const result = await confirmRequirement(store.currentRequirementId)
    alert(result.message)
    resetAndGoToList()
  } catch (e: any) {
    alert('确认失败: ' + e.message)
  }
}

// 转给助理
async function transferToDesignAssistant() {
  if (!store.currentRequirementId) return

  try {
    const result = await transferToAssistant({
      requirementId: store.currentRequirementId,
    })
    alert(result.message)
    resetAndGoToList()
  } catch (e: any) {
    alert('转交失败: ' + e.message)
  }
}

// 继续对话
function continueChat() {
  currentView.value = 'chat'
}

// 重置并返回列表
function resetAndGoToList() {
  store.reset()
  sessionInitialized.value = false
  router.push('/plugins/ai-design-assistant/list')
}

// 滚动到底部
function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 面料商信息接口
interface SupplierInfo {
  supplierTenantId: number
  supplierName: string
  status: string
}

// 加载已接受合作的供应商
async function loadSuppliers() {
  try {
    const response = await fetch(
      '/api/supplier/collaboration/list?status=accepted&page=1&size=100',
    )
    const result = await response.json()
    if (result.code === 0) {
      // 后端返回 snake_case，前端需要适配
      availableSuppliers.value = result.data.collaborations.map((c: any) => ({
        supplierTenantId: c.supplier_tenant_id,
        supplierName: c.supplier_name,
        status: c.status
      }))
    }
  } catch (error) {
    console.error('加载面料商失败', error)
  }
}

// 格式化余额
const formattedBalance = computed(() => balance.value.toLocaleString())

// 是否显示开始引导
const showWelcome = computed(() => store.conversationHistory.length === 0)
</script>

<template>
  <div class="design-requirement-create page-container">
    <header class="header">
      <div class="header-left">
        <button @click="resetAndGoToList" class="back-btn">← 返回</button>
        <h1>AI 设计助手</h1>
      </div>
      <div class="header-right">
        <div class="balance">
          余额: <span class="balance-value">{{ formattedBalance }} Tokens</span>
        </div>
        <div v-if="availableSuppliers.length > 0 && !sessionInitialized" class="supplier-selector">
          <el-select
            v-model="selectedSupplierId"
            placeholder="选择面料商（可选）"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="supplier in availableSuppliers"
              :key="supplier.supplierTenantId"
              :label="supplier.supplierName"
              :value="supplier.supplierTenantId"
            />
          </el-select>
        </div>
        <button
          v-if="sessionInitialized && currentView === 'chat'"
          @click="generateSummary"
          class="btn btn-primary"
          :disabled="store.isSummarizing"
        >
          {{ store.isSummarizing ? '生成中...' : '生成总结' }}
        </button>
      </div>
    </header>

    <main class="content">
      <!-- 对话视图 -->
      <div v-if="currentView === 'chat'" class="chat-view">
        <!-- 欢迎界面 -->
        <div v-if="showWelcome" class="welcome-view">
          <div class="welcome-content">
            <div class="welcome-icon">👋</div>
            <h2>你好！我是你的 AI 设计助手</h2>
            <p>告诉我你的设计需求，我会帮你整理并生成结构化的设计方案</p>
            <div class="welcome-tips">
              <div class="tip">💬 直接描述你的需求，如"要做一款秋季连衣裙"</div>
              <div class="tip">🖼️ 上传参考图片或视频</div>
              <div class="tip"><el-icon :size="14"><Microphone /></el-icon> 点击麦克风使用语音输入</div>
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <div
          v-show="!showWelcome"
          ref="messagesContainer"
          class="chat-messages"
          @scroll="scrollToBottom"
        >
          <div
            v-for="(msg, i) in store.conversationHistory"
            :key="i"
            :class="['message', msg.role]"
          >
            <div class="message-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
            <div class="message-body">
              <div class="message-content">{{ msg.content }}</div>
              <div class="message-time">{{ new Date(msg.time).toLocaleTimeString() }}</div>
            </div>
          </div>
          <div v-if="store.isChatting" class="message assistant typing">
            <div class="message-avatar">🤖</div>
            <div class="message-body">
              <div class="message-content">
                <span class="typing-indicator">•••</span> AI 正在思考...
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <!-- 附件栏 -->
          <div class="attachment-bar">
            <button @click="$refs.imageInput.click()" class="attachment-btn" title="上传图片">
              📷 图片
            </button>
            <input
              ref="imageInput"
              type="file"
              accept="image/*"
              multiple
              @change="handleImageUpload"
              class="file-input hidden"
            />
            <button @click="$refs.videoInput.click()" class="attachment-btn" title="上传视频">
              🎥 视频
            </button>
            <input
              ref="videoInput"
              type="file"
              accept="video/*"
              multiple
              @change="handleVideoUpload"
              class="file-input hidden"
            />
            <div v-if="store.uploadedImages.length > 0 || store.uploadedVideos.length > 0" class="attachment-preview">
              <span>{{ store.uploadedImages.length }} 图片, {{ store.uploadedVideos.length }} 视频</span>
            </div>
          </div>

          <!-- 输入框 -->
          <div class="input-wrapper">
            <button
              @click="toggleRecording"
              :class="['mic-btn', { recording: isRecording }]"
              title="语音输入"
            >
              <el-icon :size="18"><Microphone /></el-icon>
            </button>
            <textarea
              v-model="userInput"
              rows="1"
              placeholder="描述你的设计需求..."
              @keydown.enter.prevent="!isRecording && sendMessage()"
              class="input-textarea"
            ></textarea>
            <button
              @click="sendMessage"
              :disabled="!userInput.trim() || store.isChatting"
              class="send-btn"
            >
              <el-icon :size="20"><Promotion /></el-icon>
            </button>
          </div>

          <!-- 语音实时转录 -->
          <div v-if="isRecording && interimTranscript" class="transcript-preview">
            {{ interimTranscript }}
          </div>
        </div>
      </div>

      <!-- 总结视图 -->
      <div v-if="currentView === 'summary'" class="summary-view">
        <div class="summary-header">
          <h2>需求总结</h2>
          <button @click="continueChat" class="btn btn-secondary">继续对话</button>
        </div>

        <div class="summary-content">
          <pre>{{ store.summaryText }}</pre>
        </div>

        <div v-if="store.aiSummary" class="summary-details">
          <div class="summary-section">
            <h3>面料要求</h3>
            <div class="summary-grid">
              <div class="summary-item">
                <label>类型</label>
                <span>{{ store.aiSummary.fabric?.type || '-' }}</span>
              </div>
              <div class="summary-item">
                <label>克重</label>
                <span>{{ store.aiSummary.fabric?.weight || '-' }}</span>
              </div>
              <div class="summary-item">
                <label>颜色</label>
                <span>{{ store.aiSummary.fabric?.color || '-' }}</span>
              </div>
              <div class="summary-item">
                <label>特殊要求</label>
                <span>{{ store.aiSummary.fabric?.special_requirements || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="summary-section">
            <h3>版型要求</h3>
            <div class="summary-grid">
              <div class="summary-item">
                <label>领型</label>
                <span>{{ store.aiSummary.pattern?.collar || '-' }}</span>
              </div>
              <div class="summary-item">
                <label>袖型</label>
                <span>{{ store.aiSummary.pattern?.sleeve || '-' }}</span>
              </div>
              <div class="summary-item">
                <label>腰型</label>
                <span>{{ store.aiSummary.pattern?.waist || '-' }}</span>
              </div>
              <div class="summary-item">
                <label>廓形</label>
                <span>{{ store.aiSummary.pattern?.silhouette || '-' }}</span>
              </div>
              <div class="summary-item full-width">
                <label>其他细节</label>
                <span>{{ store.aiSummary.pattern?.other_details || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="summary-section">
            <h3>其他信息</h3>
            <div class="summary-grid">
              <div class="summary-item">
                <label>数量</label>
                <span>{{ store.aiSummary.quantity || '-' }}</span>
              </div>
              <div class="summary-item">
                <label>截止日期</label>
                <span>{{ store.aiSummary.deadline || '-' }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="summary-actions">
          <button @click="transferToDesignAssistant" class="btn btn-secondary">
            转给设计助理
          </button>
          <button @click="confirmAndPublish" class="btn btn-primary">
            确认并发布任务
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.design-requirement-create {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: hsl(var(--secondary));
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: hsl(var(--card));
  border-bottom: 1px solid hsl(var(--border));
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: hsl(var(--muted-foreground));
  padding: 4px 8px;
}

.back-btn:hover {
  color: hsl(var(--foreground));
}

.header h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.balance {
  font-size: 14px;
  color: hsl(var(--muted-foreground));
}

.balance-value {
  font-weight: 600;
  color: hsl(160 84% 40%);
}

.supplier-selector {
  display: flex;
  align-items: center;
}

.content {
  flex: 1;
  overflow: hidden;
  max-width: 1000px;
  width: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
}

/* 对话视图 */
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

/* 欢迎界面 */
.welcome-view {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.welcome-content {
  text-align: center;
  max-width: 500px;
}

.welcome-icon {
  font-size: 64px;
  margin-bottom: 24px;
}

.welcome-content h2 {
  margin: 0 0 12px;
  font-size: 28px;
  color: hsl(var(--foreground));
}

.welcome-content > p {
  margin: 0 0 32px;
  font-size: 16px;
  color: hsl(var(--muted-foreground));
}

.welcome-tips {
  text-align: left;
  background: hsl(var(--card));
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.tip {
  padding: 12px 0;
  border-bottom: 1px solid hsl(var(--secondary));
  font-size: 14px;
  color: hsl(var(--foreground));
}

.tip:last-child {
  border-bottom: none;
}

/* 消息列表 */
.chat-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px 20px 40px;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: hsl(var(--secondary));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.message-body {
  max-width: 80%;
}

.message-content {
  background: hsl(var(--card));
  border-radius: 12px;
  padding: 12px 16px;
  line-height: 1.6;
  font-size: 15px;
  color: hsl(var(--foreground));
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.message.user .message-content {
  background: hsl(160 84% 40%);
  color: hsl(var(--primary-foreground));
  border-radius: 12px 12px 0 12px;
}

.message.assistant .message-content {
  background: hsl(var(--card));
  border-radius: 12px 12px 12px 0;
}

.message.typing .message-content {
  padding: 8px 12px;
}

.typing-indicator {
  display: inline-block;
  animation: typingBounce 1.4s infinite;
}

@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-4px); }
}

.message-time {
  font-size: 11px;
  color: hsl(var(--muted-foreground));
  margin-top: 6px;
}

.message.user .message-time {
  text-align: right;
}

/* 输入区域 */
.input-area {
  flex-shrink: 0;
  background: hsl(var(--card));
  border-top: 1px solid hsl(var(--border));
  padding: 16px 20px 20px;
}

.attachment-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.attachment-btn {
  padding: 6px 12px;
  background: hsl(var(--secondary));
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.attachment-btn:hover {
  background: hsl(var(--border));
}

.attachment-preview {
  margin-left: auto;
  font-size: 13px;
  color: hsl(var(--muted-foreground));
  background: hsl(var(--secondary));
  padding: 6px 12px;
  border-radius: 6px;
}

.hidden {
  display: none;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: hsl(var(--secondary));
  border-radius: 12px;
  padding: 8px;
}

.mic-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: hsl(var(--border));
  border: none;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  padding: 0;
}

.mic-btn.recording {
  background: hsl(var(--destructive) / 0.1);
  color: hsl(var(--destructive));
  animation: recordingPulse 1.5s infinite;
}

@keyframes recordingPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(220, 38, 38, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(220, 38, 38, 0); }
}

.input-textarea {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  padding: 8px 0;
  font-family: inherit;
  max-height: 150px;
}

.input-textarea:focus {
  outline: none;
}

.input-textarea::placeholder {
  color: hsl(var(--muted-foreground));
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: hsl(160 84% 40%);
  border: none;
  color: hsl(var(--primary-foreground));
  cursor: pointer;
  transition: background 0.2s;
  flex-shrink: 0;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn:hover:not(:disabled) {
  background: hsl(160 84% 35%);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.transcript-preview {
  margin-top: 8px;
  padding: 8px 12px;
  background: hsl(38 92% 55% / 0.15);
  color: hsl(38 92% 55%);
  font-size: 14px;
  border-radius: 6px;
}

/* 总结视图 */
.summary-view {
  height: 100%;
  overflow-y: auto;
  padding: 24px 20px;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.summary-header h2 {
  margin: 0;
  font-size: 20px;
  color: hsl(var(--foreground));
}

.summary-content {
  background: hsl(var(--card));
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.summary-content pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
  font-size: 14px;
  line-height: 1.6;
  color: hsl(var(--foreground));
  margin: 0;
}

.summary-details {
  background: hsl(var(--card));
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.summary-section {
  margin-bottom: 28px;
}

.summary-section:last-child {
  margin-bottom: 0;
}

.summary-section h3 {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--foreground));
  padding-bottom: 8px;
  border-bottom: 2px solid hsl(var(--secondary));
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-item.full-width {
  grid-column: span 2;
}

.summary-item label {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  font-weight: 500;
  text-transform: uppercase;
}

.summary-item span {
  font-size: 14px;
  color: hsl(var(--foreground));
}

.summary-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid hsl(var(--border));
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: hsl(160 84% 40%);
  color: hsl(var(--primary-foreground));
}

.btn-primary:hover:not(:disabled) {
  background: hsl(160 84% 35%);
}

.btn-secondary {
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
}

.btn-secondary:hover:not(:disabled) {
  background: hsl(var(--border));
}
</style>