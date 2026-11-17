<template>
  <div>
    <!-- 评价汇总 -->
    <el-row :gutter="16" class="summary">
      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="score-card">
          <div class="score-main">
            <div class="score-value">{{ Number(summary.avgRating || 0).toFixed(1) }}</div>
            <div class="score-side">
              <el-rate :model-value="Number(summary.avgRating || 0)" disabled />
              <div class="score-label">共 {{ summary.totalCount }} 条评价</div>
              <div class="score-label">好评率 {{ summary.goodRate }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="dim-card">
          <div v-for="d in dims" :key="d.label" class="dim-row">
            <span class="dim-label">{{ d.label }}</span>
            <el-progress :percentage="(d.value / 5) * 100" :show-text="false" :stroke-width="6" color="#e6a23c" class="dim-bar" />
            <span class="dim-value">{{ d.value.toFixed(1) }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="tag-card">
          <div class="card-title">大家怎么形容你</div>
          <div v-if="!summary.tags.length" class="empty-tip">还没有评价标签</div>
          <div class="tag-cloud">
            <el-tag v-for="t in summary.tags" :key="t.name" type="warning" effect="plain" size="small">
              {{ t.name }} {{ t.count }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div class="panel-head">
          <span>评价中心</span>
          <el-button link type="primary" @click="$router.push('/my/accepted')">查看我的接单 →</el-button>
        </div>
      </template>

      <el-tabs v-model="tab" @tab-change="onTabChange">
        <el-tab-pane name="pending">
          <template #label>
            待评价<span v-if="pendingTotal" class="tab-badge">{{ pendingTotal }}</span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="我收到的" name="received" />
        <el-tab-pane label="我发出的" name="sent" />
      </el-tabs>

      <!-- 待评价 -->
      <div v-if="tab === 'pending'" v-loading="loading">
        <el-empty v-if="!pendingList.length" description="没有待评价的订单，完成的订单都可以在这里评价" />
        <div v-for="p in pendingList" :key="p.taskId" class="pending-item">
          <div class="p-left">
            <div class="p-title">{{ p.title }}</div>
            <div class="p-meta">
              <el-tag size="small" :type="p.role === 'PUBLISHER' ? 'warning' : 'primary'" effect="plain">
                {{ p.role === 'PUBLISHER' ? '评价跑腿员' : '评价发布者' }}
              </el-tag>
              <span>对象：{{ p.counterpartName || '—' }}</span>
              <span>完成于 {{ p.completeTime || '—' }}</span>
            </div>
          </div>
          <div class="p-right">
            <b class="reward">¥{{ p.reward }}</b>
            <el-button type="primary" size="small" @click="openDialog(p)">去评价</el-button>
          </div>
        </div>
        <div v-if="pendingTotal > pageSize" class="pager">
          <el-pagination layout="prev, pager, next" :total="pendingTotal" :page-size="pageSize" :current-page="page" @current-change="loadPending" />
        </div>
      </div>

      <!-- 我收到的 / 我发出的 -->
      <div v-else v-loading="loading">
        <el-empty v-if="!recordList.length" :description="tab === 'received' ? '还没有收到评价' : '你还没有评价过别人'" />
        <div v-for="e in recordList" :key="e.id" class="rec-item">
          <div class="rec-head">
            <el-rate :model-value="e.rating" disabled size="small" />
            <el-tag size="small" type="info" effect="plain">{{ e.taskTitle || '任务' }}</el-tag>
            <span class="rec-time">{{ e.createTime }}</span>
          </div>
          <div class="rec-tags" v-if="e.tags && e.tags.length">
            <el-tag v-for="t in e.tags" :key="t" size="small" effect="plain">{{ t }}</el-tag>
          </div>
          <div class="rec-content">{{ e.content || '（未填写文字评价）' }}</div>
          <div class="rec-foot">
            <span v-if="tab === 'received'">来自：{{ e.evaluatorName }}</span>
            <span v-else>评价对象：{{ e.evaluateeName }}</span>
            <span v-if="e.speedRating" class="dim-mini">速度 {{ e.speedRating }} · 态度 {{ e.serviceRating }} · 质量 {{ e.qualityRating }}</span>
          </div>
        </div>
        <div v-if="recordTotal > pageSize" class="pager">
          <el-pagination layout="prev, pager, next" :total="recordTotal" :page-size="pageSize" :current-page="page" @current-change="loadRecords" />
        </div>
      </div>
    </el-card>

    <RateDialog
      v-model="dialogVisible"
      :task-id="current.taskId"
      :target-name="current.counterpartName"
      :role="current.role"
      @success="onEvaluated"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  myEvaluationSummary,
  pendingEvaluations,
  receivedEvaluations,
  sentEvaluations
} from '../api/evaluation'
import RateDialog from '../components/RateDialog.vue'

const route = useRoute()

const tab = ref('pending')
const loading = ref(false)
const pageSize = 8
const page = ref(1)
const pendingList = ref([])
const pendingTotal = ref(0)
const recordList = ref([])
const recordTotal = ref(0)
const summary = reactive({ totalCount: 0, avgRating: 0, speedAvg: 0, serviceAvg: 0, qualityAvg: 0, goodRate: 0, tags: [] })
const dialogVisible = ref(false)
const current = reactive({ taskId: null, counterpartName: '', role: 'PUBLISHER' })

const dims = computed(() => [
  { label: '配送速度', value: Number(summary.speedAvg || 0) },
  { label: '服务态度', value: Number(summary.serviceAvg || 0) },
  { label: '完成质量', value: Number(summary.qualityAvg || 0) }
])

async function loadSummary() {
  Object.assign(summary, await myEvaluationSummary())
}

async function loadPending(p = 1) {
  loading.value = true
  try {
    page.value = p
    const data = await pendingEvaluations({ page: p, size: pageSize })
    pendingList.value = data.records || []
    pendingTotal.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadRecords(p = 1) {
  loading.value = true
  try {
    page.value = p
    const api = tab.value === 'received' ? receivedEvaluations : sentEvaluations
    const data = await api({ page: p, size: pageSize })
    recordList.value = data.records || []
    recordTotal.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function onTabChange(name) {
  page.value = 1
  if (name === 'pending') loadPending(1)
  else loadRecords(1)
}

function openDialog(p) {
  current.taskId = p.taskId
  current.counterpartName = p.counterpartName
  current.role = p.role
  dialogVisible.value = true
}

function onEvaluated() {
  loadPending(page.value)
  loadSummary()
}

onMounted(async () => {
  await loadPending(1)
  loadSummary()
  // 支持从别处带任务ID直达评价弹窗：/evaluations?taskId=12
  const taskId = route.query.taskId
  if (taskId) {
    const target = pendingList.value.find((p) => String(p.taskId) === String(taskId))
    if (target) openDialog(target)
  }
})
</script>

<style scoped>
.summary {
  margin-bottom: 16px;
}
.score-card,
.dim-card,
.tag-card {
  height: 100%;
  margin-bottom: 8px;
}
.score-main {
  display: flex;
  align-items: center;
  gap: 16px;
}
.score-value {
  font-size: 42px;
  font-weight: 800;
  color: #f0a020;
  line-height: 1;
}
.score-label {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.dim-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
}
.dim-label {
  width: 64px;
  font-size: 13px;
  color: #606266;
}
.dim-bar {
  flex: 1;
}
.dim-value {
  width: 30px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
  color: #e6a23c;
}
.card-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.empty-tip {
  font-size: 12px;
  color: #c0c4cc;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.tab-badge {
  display: inline-block;
  margin-left: 4px;
  background: #f56c6c;
  color: #fff;
  border-radius: 8px;
  padding: 0 6px;
  font-size: 12px;
  line-height: 18px;
}
.pending-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 10px;
}
.pending-item:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
}
.p-title {
  font-weight: 600;
  color: #303133;
}
.p-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
.p-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.reward {
  color: #f56c6c;
  font-size: 17px;
}
.rec-item {
  border-bottom: 1px dashed #ebeef5;
  padding: 12px 0;
}
.rec-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.rec-time {
  margin-left: auto;
  font-size: 12px;
  color: #c0c4cc;
}
.rec-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin: 6px 0;
}
.rec-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}
.rec-foot {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
.dim-mini {
  color: #c0c4cc;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}
</style>
