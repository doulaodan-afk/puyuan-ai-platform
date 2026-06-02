<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  getFabricLibraryList,
  createFabric,
  updateFabric,
  deleteFabric as deleteFabricApi,
} from '../api'
import { useDesignAssistantStore } from '../stores'
import type { FabricLibraryItem } from '../types'

const store = useDesignAssistantStore()

const categoryFilter = ref('')
const showHidden = ref(false)

const dialogVisible = ref(false)
const isEditMode = ref(false)
const form = ref({
  id: 0,
  name: '',
  category: '',
  images: [] as string[],
  videoUrl: '',
  specs: '{}',
  pricePerMeter: 0,
  stockStatus: 'in_stock' as 'in_stock' | 'out_of_stock',
  isVisible: 1,
})

const imagePreviews = ref<string[]>([])

const loading = ref(false)
const submitting = ref(false)

const fabrics = computed(() => {
  let filtered = store.myFabrics

  if (categoryFilter.value) {
    filtered = filtered.filter(f => f.category === categoryFilter.value)
  }

  if (!showHidden.value) {
    filtered = filtered.filter(f => f.isVisible === 1)
  }

  return filtered
})

const categoryStats = computed(() => {
  const stats: Record<string, number> = {}
  store.myFabrics.forEach(f => {
    const cat = f.category || '未分类'
    stats[cat] = (stats[cat] || 0) + 1
  })
  return stats
})

const totalCount = computed(() => store.myFabrics.length)
const visibleCount = computed(() => store.myFabrics.filter(f => f.isVisible === 1).length)

onMounted(async () => {
  await fetchFabrics()
})

async function fetchFabrics() {
  loading.value = true
  try {
    const response = await getFabricLibraryList({
      category: categoryFilter.value || undefined,
      onlyVisible: false,
      page: 1,
      size: 200,
    })
    store.setMyFabrics(response.fabrics)
  } catch (e: any) {
    console.error('获取面料库失败', e)
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  isEditMode.value = false
  form.value = {
    id: 0,
    name: '',
    category: '',
    images: [],
    videoUrl: '',
    specs: '{}',
    pricePerMeter: 0,
    stockStatus: 'in_stock',
    isVisible: 1,
  }
  imagePreviews.value = []
  dialogVisible.value = true
}

function openEditDialog(fabric: FabricLibraryItem) {
  isEditMode.value = true
  form.value = {
    id: fabric.id,
    name: fabric.name,
    category: fabric.category || '',
    images: fabric.images || [],
    videoUrl: fabric.videoUrl || '',
    specs: JSON.stringify(fabric.specs || {}, null, 2),
    pricePerMeter: fabric.pricePerMeter || 0,
    stockStatus: fabric.stockStatus,
    isVisible: fabric.isVisible,
  }
  imagePreviews.value = [...fabric.images]
  dialogVisible.value = true
}

async function save() {
  if (!form.value.name.trim()) {
    alert('请填写面料名称')
    return
  }

  submitting.value = true
  try {
    JSON.parse(form.value.specs)

    if (isEditMode.value) {
      await updateFabric(form.value.id, {
        name: form.value.name,
        category: form.value.category || undefined,
        images: form.value.images,
        videoUrl: form.value.videoUrl || undefined,
        specs: JSON.parse(form.value.specs),
        pricePerMeter: form.value.pricePerMeter || undefined,
        stockStatus: form.value.stockStatus,
        isVisible: form.value.isVisible,
      })
      store.updateFabric({ ...form.value, specs: JSON.parse(form.value.specs) })
      alert('更新成功')
    } else {
      const newFabric = await createFabric({
        name: form.value.name,
        category: form.value.category || undefined,
        images: form.value.images,
        videoUrl: form.value.videoUrl || undefined,
        specs: JSON.parse(form.value.specs),
        pricePerMeter: form.value.pricePerMeter || undefined,
        stockStatus: form.value.stockStatus,
      })
      store.addFabric(newFabric)
      alert('添加成功')
    }

    dialogVisible.value = false
    await fetchFabrics()
  } catch (e: any) {
    if (e.message?.includes('JSON')) {
      alert('规格 JSON 格式错误，请检查语法')
    } else {
      alert('保存失败: ' + e.message)
    }
  } finally {
    submitting.value = false
  }
}

async function removeFabric(id: number) {
  if (!confirm('确认下架此面料？')) return

  try {
    await deleteFabricApi(id)
    store.removeFabric(id)
    alert('已下架')
    await fetchFabrics()
  } catch (e: any) {
    alert('操作失败: ' + e.message)
  }
}

function handleImageUpload(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files && input.files[0]) {
    const file = input.files[0]
    const url = URL.createObjectURL(file)
    imagePreviews.value.push(url)
    form.value.images = [...imagePreviews.value]
  }
}

function removeImage(index: number) {
  imagePreviews.value.splice(index, 1)
  form.value.images = [...imagePreviews.value]
}

function getStockStatusLabel(status: string) {
  return status === 'in_stock' ? '有货' : '缺货'
}

function getStockClass(status: string) {
  return status === 'in_stock' ? 'in-stock' : 'out-of-stock'
}

function toggleVisibility(fabric: FabricLibraryItem) {
  const newStatus = fabric.isVisible === 1 ? 0 : 1
  fabric.isVisible = newStatus
}

const categoryOptions = ['真丝', '羊毛', '棉麻', '化纤', '混纺', '皮革', '其他']
</script>

<template>
  <div class="fabric-manage-page page-container">
    <header class="header">
      <h1>面料库管理</h1>
      <button @click="openCreateDialog" class="btn btn-primary">
        + 新增面料
      </button>
    </header>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">总数量</div>
        <div class="stat-value">{{ totalCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">可见数量</div>
        <div class="stat-value">{{ visibleCount }}</div>
      </div>
      <div v-for="(count, cat) in categoryStats" :key="cat" class="stat-card">
        <div class="stat-label">{{ cat }}</div>
        <div class="stat-value">{{ count }}</div>
      </div>
    </div>

    <div class="filters">
      <select v-model="categoryFilter" @change="fetchFabrics" class="filter-select">
        <option value="">全部分类</option>
        <option v-for="cat in categoryOptions" :key="cat" :value="cat">
          {{ cat }}
        </option>
      </select>
      <label class="checkbox-label">
        <input v-model="showHidden" type="checkbox" @change="fetchFabrics" />
        显示已下架
      </label>
      <button @click="fetchFabrics" class="btn btn-secondary">刷新</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="fabrics.length === 0" class="empty">
      <div class="empty-icon">🧵</div>
      <p>暂无面料</p>
      <button @click="openCreateDialog" class="btn btn-secondary">添加第一个面料</button>
    </div>

    <div v-else class="fabric-grid">
      <div v-for="fabric in fabrics" :key="fabric.id" class="fabric-card">
        <div class="card-image">
          <img v-if="fabric.images?.[0]" :src="fabric.images[0]" :alt="fabric.name" />
          <div v-else class="no-image">🧵</div>
          <div v-if="fabric.images?.length > 1" class="image-count">+{{ fabric.images.length - 1 }}</div>
        </div>

        <div class="card-content">
          <div class="card-header">
            <h3>{{ fabric.name }}</h3>
            <button
              @click.stop="toggleVisibility(fabric)"
              :class="['visibility-btn', { hidden: fabric.isVisible === 0 }]"
              :title="fabric.isVisible === 1 ? '隐藏' : '显示'"
            >
              👁️
            </button>
          </div>

          <div class="card-meta">
            <div v-if="fabric.category" class="tag category">{{ fabric.category }}</div>
            <div :class="['tag stock', getStockClass(fabric.stockStatus)]">
              {{ getStockStatusLabel(fabric.stockStatus) }}
            </div>
            <div v-if="fabric.pricePerMeter" class="price">¥{{ fabric.pricePerMeter }}/米</div>
          </div>

          <div v-if="Object.keys(fabric.specs).length > 0" class="specs-preview">
            <div v-for="(val, key) in fabric.specs" :key="key" class="spec-item">
              <span class="spec-key">{{ key }}:</span>
              <span class="spec-val">{{ val }}</span>
            </div>
          </div>

          <div v-if="fabric.videoUrl" class="video-indicator">🎥 含视频</div>
        </div>

        <div class="card-actions">
          <button @click.stop="openEditDialog(fabric)" class="btn btn-secondary">编辑</button>
          <button @click.stop="removeFabric(fabric.id)" class="btn btn-danger">下架</button>
        </div>
      </div>
    </div>

    <dialog v-if="dialogVisible" class="dialog-overlay" @click.self="dialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h2>{{ isEditMode ? '编辑面料' : '新增面料' }}</h2>
          <button @click="dialogVisible = false" class="close-btn">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>面料名称 *</label>
            <input v-model="form.name" type="text" class="input" placeholder="如：重磅真丝" />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>品类</label>
              <select v-model="form.category" class="input">
                <option value="">请选择</option>
                <option v-for="cat in categoryOptions" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </div>
            <div class="form-group">
              <label>库存状态</label>
              <select v-model="form.stockStatus" class="input">
                <option value="in_stock">有货</option>
                <option value="out_of_stock">缺货</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label>单价 (元/米)</label>
            <input v-model="form.pricePerMeter" type="number" min="0" step="0.01" class="input" placeholder="0.00" />
          </div>

          <div class="form-group">
            <label>视频 URL</label>
            <input v-model="form.videoUrl" type="text" class="input" placeholder="面料小样视频地址" />
          </div>

          <div class="form-group">
            <label>图片</label>
            <input type="file" accept="image/*" @change="handleImageUpload" class="file-input" />
            <div v-if="imagePreviews.length > 0" class="image-previews">
              <div v-for="(img, i) in imagePreviews" :key="i" class="preview-item">
                <img :src="img" alt="" />
                <button @click="removeImage(i)" class="remove-btn">×</button>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label>规格参数 (JSON)</label>
            <textarea v-model="form.specs" rows="6" class="input textarea" placeholder='{"克重": "20mm", "门幅": "150cm", "成分": "100%真丝"}'></textarea>
          </div>

          <div class="form-group">
            <label>是否在前端展示</label>
            <select v-model="form.isVisible" class="input">
              <option :value="1">是</option>
              <option :value="0">否</option>
            </select>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="dialogVisible = false" class="btn btn-secondary">取消</button>
          <button @click="save" :disabled="submitting" class="btn btn-primary">
            {{ submitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </dialog>
  </div>
</template>

<style scoped>
.fabric-manage-page { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header h1 { margin: 0; color: hsl(var(--foreground)); font-size: 24px; }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 16px; margin-bottom: 24px; }
.stat-card { background: hsl(var(--card)); border: 1px solid hsl(var(--border)); border-radius: 12px; padding: 20px; text-align: center; }
.stat-label { font-size: 13px; color: hsl(var(--muted-foreground)); margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; color: hsl(var(--foreground)); }
.filters { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.filter-select { padding: 8px 12px; border: 1px solid hsl(var(--border)); border-radius: 6px; font-size: 14px; background: hsl(var(--card)); cursor: pointer; }
.checkbox-label { display: flex; align-items: center; gap: 6px; font-size: 14px; color: hsl(var(--foreground)); }
.loading { text-align: center; padding: 40px; color: hsl(var(--muted-foreground)); }
.empty { text-align: center; padding: 80px 20px; color: hsl(var(--muted-foreground)); }
.empty-icon { font-size: 64px; margin-bottom: 16px; }
.fabric-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.fabric-card { background: hsl(var(--card)); border: 1px solid hsl(var(--border)); border-radius: 12px; overflow: hidden; transition: all 0.2s; }
.fabric-card:hover { box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); }
.fabric-card.hidden { opacity: 0.6; }
.card-image { position: relative; aspect-ratio: 16/10; background: hsl(var(--secondary)); overflow: hidden; }
.card-image img { width: 100%; height: 100%; object-fit: cover; }
.no-image { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; font-size: 48px; color: hsl(var(--border)); }
.image-count { position: absolute; bottom: 8px; right: 8px; background: hsl(var(--foreground) / 0.7); color: white; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.card-content { padding: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
.card-header h3 { margin: 0; color: hsl(var(--foreground)); font-size: 16px; line-height: 1.4; }
.visibility-btn { background: none; border: none; font-size: 18px; cursor: pointer; padding: 0; opacity: 0.6; }
.visibility-btn:hover { opacity: 1; }
.visibility-btn.hidden { opacity: 0.3; }
.card-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.tag { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.tag.category { background: hsl(var(--secondary)); color: hsl(var(--muted-foreground)); }
.tag.stock { font-weight: 500; }
.tag.stock.in-stock { background: hsl(142 71% 50% / 0.15); color: hsl(142 71% 50%); }
.tag.stock.out-of-stock { background: hsl(var(--destructive) / 0.15); color: hsl(var(--destructive)); }
.price { margin-left: auto; font-size: 14px; font-weight: 500; color: hsl(160 84% 40%); }
.specs-preview { display: grid; grid-template-columns: 1fr 1fr; gap: 4px; padding: 8px; background: hsl(var(--secondary)); border-radius: 6px; margin-bottom: 8px; }
.spec-item { display: flex; font-size: 12px; color: hsl(var(--muted-foreground)); }
.spec-key { color: hsl(var(--muted-foreground)); }
.spec-val { font-weight: 500; }
.video-indicator { font-size: 12px; color: hsl(var(--muted-foreground)); margin-bottom: 12px; }
.card-actions { display: flex; gap: 8px; padding-top: 12px; border-top: 1px solid hsl(var(--border)); }
.btn { padding: 8px 16px; border: none; border-radius: 6px; font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.2s; }
.btn:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-primary { background: hsl(160 84% 40%); color: hsl(var(--primary-foreground)); }
.btn-primary:hover:not(:disabled) { background: hsl(160 84% 35%); }
.btn-secondary { background: hsl(var(--secondary)); color: hsl(var(--foreground)); }
.btn-secondary:hover:not(:disabled) { background: hsl(var(--border)); }
.btn-danger { background: hsl(var(--destructive) / 0.1); color: hsl(var(--destructive)); }
.btn-danger:hover { background: hsl(var(--destructive) / 0.15); }
.dialog-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: hsl(var(--foreground) / 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog { background: hsl(var(--card)); border-radius: 12px; width: 100%; max-width: 600px; max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1); }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 20px; border-bottom: 1px solid hsl(var(--border)); }
.dialog-header h2 { margin: 0; font-size: 18px; color: hsl(var(--foreground)); }
.close-btn { background: none; border: none; font-size: 24px; color: hsl(var(--muted-foreground)); cursor: pointer; padding: 0; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; }
.dialog-body { padding: 20px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; font-size: 14px; font-weight: 500; color: hsl(var(--foreground)); }
.input { width: 100%; padding: 10px 12px; border: 1px solid hsl(var(--border)); border-radius: 6px; font-size: 14px; }
.input:focus { outline: none; border-color: hsl(160 84% 40%); box-shadow: 0 0 0 3px hsl(160 84% 40% / 0.1); }
.input.textarea { font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace; resize: vertical; }
.file-input { margin-bottom: 8px; }
.image-previews { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.preview-item { position: relative; aspect-ratio: 1; }
.preview-item img { width: 100%; height: 100%; object-fit: cover; border-radius: 4px; }
.remove-btn { position: absolute; top: 4px; right: 4px; width: 20px; height: 20px; background: hsl(var(--foreground) / 0.6); color: hsl(var(--primary-foreground)); border: none; border-radius: 50%; cursor: pointer; font-size: 14px; line-height: 1; }
.remove-btn:hover { background: hsl(var(--destructive) / 0.9); }
.dialog-footer { display: flex; justify-content: flex-end; gap: 8px; padding: 20px; border-top: 1px solid hsl(var(--border)); }
</style>