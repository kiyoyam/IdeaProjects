<template>
  <div v-loading="loading">
    <!-- 欢迎横幅 -->
    <el-card shadow="never" class="hero">
      <div class="hero-inner">
        <div class="hero-left">
          <div class="hello">
            你好，{{ store.user?.nickname || store.user?.username || '同学' }}
            <el-tag size="small" effect="dark" round style="margin-left: 8px">{{ roleLabel(store.user?.role) }}</el-tag>
          </div>
          <div class="sub">
            今天已经接了 <b>{{ overview.todayCount }}</b> 单，手上还有 <b>{{ overview.activeCount }}</b> 单待处理
          </div>
          <div class="hero-actions">
            <el-button type="primary" @click="$router.push('/tasks')">去任务广场</el-button>
            <el-button plain @click="$router.push('/publish')">发布任务</el-button>
          </div>
        </div>
        <div class="hero-right">
          <div class="credit">
            <div class="credit-label">信用评分</div>
            <div class="credit-value">{{ Number(overview.rating || 0).toFixed(1) }}</div>
            <el-rate :model-value="Number(overview.rating || 0)" disabled size="small" />
            <div class="credit-tip">{{ overview.ratingCount }} 次评价</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 数据卡片 -->
    <el-row :gutter="16" class="stats">
      <el-col v-for="c in statCards" :key="c.label" :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: c.bg, color: c.color }">
            <el-icon :size="20"><component :is="c.icon" /></el-icon>
          </div>
          <div>
            <div class="stat-value" :style="{ color: c.color }">{{ c.value }}</div>
            <div class="stat-label">{{ c.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 进行中的接单 -->
      <el-col :xs="24" :md="15">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-head">
              <span>我进行中的接单</span>
              <el-button link type="primary" @click="$router.push('/my/accepted')">全部接单 →</el-button>
            </div>
          </template>
          <el-empty v-if="!activeOrders.length" description="手上的单都处理完了，去广场接一单吧">
            <el-button type="primary" @click="$router.push('/tasks')">去接单</el-button>
          </el-empty>
          <div v-for="o in activeOrders" :key="o.id" class="order-item">
            <div class="order-top">
              <div class="order-title" @click="$router.push(`/task/${o.taskId}`)">{{ o.title }}</div>
              <b class="order-reward">¥{{ o.reward }}</b>
            </div>
            <div class="order-meta">
              <el-tag :type="statusType(o.status)" size="small">{{ statusLabel(o.status) }}</el-tag>
              <span>发布者：{{ o.publisherName }}</span>
              <span v-if="o.deadline" :class="{ urgent: isUrgent(o.deadline) }">
                {{ countdownText(o.deadline) }}
              </span>
            </div>
            <el-progress
              :percentage="(statusStep(o.status) / 4) * 100"
              :show-text="false"
              :stroke-width="6"
              :color="progressColor"
              class="order-progress"
            />
            <div class="order-addr">
              <div><el-icon><Location /></el-icon> 取：{{ o.pickupAddress }}</div>
              <div><el-icon><Position /></el-icon> 送：{{ o.deliveryAddress }}</div>
            </div>
            <div class="order-foot">
              <el-button size="small" @click="$router.push(`/task/${o.taskId}`)">详情</el-button>
              <el-button v-if="o.status === 'ACCEPTED'" size="small" type="primary" @click="doStart(o)">开始配送</el-button>
              <el-button v-if="o.status === 'IN_PROGRESS'" size="small" type="success" @click="doFinish(o)">标记送达</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 广场概览 + 推荐 -->
      <el-col :xs="24" :md="9">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-head">
              <span>广场速览</span>
            </div>
          </template>
          <div class="glance">
            <div class="glance-item">
              <div class="glance-value">{{ overview.pendingCount }}</div>
              <div class="glance-label">待接单</div>
            </div>
            <div class="glance-item">
              <div class="glance-value">{{ overview.todayCompleted }}</div>
              <div class="glance-label">今日完成</div>
            </div>
          </div>
          <el-divider />
          <div class="panel-head mini">
            <span>推荐任务</span>
            <el-button link type="primary" @click="$router.push('/tasks')">更多 →</el-button>
          </div>
          <el-empty v-if="!recommend.length" description="暂时没有待接单的任务" :image-size="60" />
          <div v-for="t in recommend" :key="t.id" class="rec-item" @click="$router.push(`/task/${t.id}`)">
            <div class="rec-head">
              <el-icon><component :is="categoryIcon(t.category)" /></el-icon>
              <span class="rec-title">{{ t.title }}</span>
              <b class="rec-reward">¥{{ t.reward }}</b>
            </div>
            <div class="rec-meta">
              <span>{{ categoryLabel(t.category) }}</span>
              <span v-if="t.deadline">{{ countdownText(t.deadline) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { myOrders, queryTasks, startTask, finishTask, taskOverview } from '../api/task'
import { useUserStore } from '../store/user'
import {
  categoryIcon,
  categoryLabel,
  countdownText,
  isUrgent,
  roleLabel,
  statusLabel,
  statusStep,
  statusType
} from '../utils/status'

const store = useUserStore()
const router = useRouter()
const loading = ref(false)
const activeOrders = ref([])
const recommend = ref([])
const overview = reactive({
  activeCount: 0,
  completedCount: 0,
  cancelledCount: 0,
  todayCount: 0,
  income: 0,
  rating: 0,
  ratingCount: 0,
  publishedActive: 0,
  publishedTotal: 0,
  pendingCount: 0,
  todayCompleted: 0
})

const progressColor = [
  { color: '#f0a020', percentage: 30 },
  { color: '#409eff', percentage: 60 },
  { color: '#67c23a', percentage: 100 }
]

const statCards = computed(() => [
  { label: '进行中', value: overview.activeCount, icon: 'Clock', color: '#409eff', bg: '#ecf5ff' },
  { label: '已完成', value: overview.completedCount, icon: 'CircleCheck', color: '#67c23a', bg: '#f0f9eb' },
  { label: '累计收入', value: `¥${Number(overview.income || 0).toFixed(2)}`, icon: 'Wallet', color: '#e6a23c', bg: '#fdf6ec' },
  { label: '我发布的', value: overview.publishedTotal, icon: 'Document', color: '#909399', bg: '#f4f4f5' }
])

async function load() {
  loading.value = true
  try {
    const [ov, orders, tasks] = await Promise.all([
      taskOverview(),
      myOrders({ page: 1, size: 5, status: 'ACTIVE' }),
      queryTasks({ page: 1, size: 4, status: 'PENDING', sort: 'reward' })
    ])
    Object.assign(overview, ov)
    activeOrders.value = orders.records || []
    recommend.value = tasks.records || []
  } finally {
    loading.value = false
  }
}

async function doStart(o) {
  await startTask(o.taskId)
  ElMessage.success('已开始配送')
  load()
}

async function doFinish(o) {
  await finishTask(o.taskId)
  ElMessage.success('已标记送达，等待发布者确认')
  load()
}

onMounted(load)
</script>

<style scoped>
.hero {
  margin-bottom: 16px;
  border: none;
  background: linear-gradient(120deg, #5b6bf5 0%, #7d8bff 45%, #9aa6ff 100%);
  color: #fff;
}
.hero-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}
.hello {
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
}
.sub {
  margin-top: 8px;
  font-size: 14px;
  opacity: 0.92;
}
.sub b {
  font-size: 18px;
  margin: 0 2px;
}
.hero-actions {
  margin-top: 16px;
  display: flex;
  gap: 10px;
}
.hero-right {
  text-align: center;
  background: rgba(255, 255, 255, 0.16);
  border-radius: 12px;
  padding: 14px 22px;
  min-width: 140px;
}
.credit-label {
  font-size: 12px;
  opacity: 0.9;
}
.credit-value {
  font-size: 30px;
  font-weight: 800;
  line-height: 1.2;
}
.credit-tip {
  font-size: 12px;
  opacity: 0.85;
}
.stats {
  margin-bottom: 16px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}
.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-label {
  font-size: 12px;
  color: #909399;
}
.panel {
  margin-bottom: 16px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.panel-head.mini {
  font-size: 14px;
  margin-bottom: 8px;
}
.order-item {
  padding: 12px 0;
  border-bottom: 1px dashed #ebeef5;
}
.order-item:last-child {
  border-bottom: none;
}
.order-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-title {
  font-weight: 600;
  color: #303133;
  cursor: pointer;
}
.order-title:hover {
  color: #5b6bf5;
}
.order-reward {
  color: #f56c6c;
  font-size: 17px;
}
.order-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  margin: 6px 0;
}
.order-meta .urgent {
  color: #f56c6c;
  font-weight: 600;
}
.order-progress {
  margin: 6px 0;
}
.order-addr {
  font-size: 12px;
  color: #606266;
  line-height: 1.7;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 6px 10px;
}
.order-foot {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.glance {
  display: flex;
  gap: 12px;
  text-align: center;
}
.glance-item {
  flex: 1;
  background: #f5f7fa;
  border-radius: 10px;
  padding: 12px 0;
}
.glance-value {
  font-size: 22px;
  font-weight: 700;
  color: #5b6bf5;
}
.glance-label {
  font-size: 12px;
  color: #909399;
}
.rec-item {
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
  cursor: pointer;
}
.rec-item:last-child {
  border-bottom: none;
}
.rec-item:hover .rec-title {
  color: #5b6bf5;
}
.rec-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}
.rec-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
}
.rec-reward {
  color: #f56c6c;
}
.rec-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  padding-left: 22px;
}
</style>
