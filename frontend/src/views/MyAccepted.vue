<template>
  <div>
    <el-row :gutter="16" class="summary">
      <el-col v-for="c in summary" :key="c.key" :span="8">
        <el-card shadow="never" class="sum-card" :class="{ active: tab === c.key }" @click="switchTab(c.key)">
          <div class="sum-value" :style="{ color: c.color }">{{ c.value }}</div>
          <div class="sum-label">{{ c.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <div class="toolbar">
        <h3>我的接单</h3>
        <el-button type="primary" plain @click="$router.push('/tasks')">去接单</el-button>
      </div>

      <el-tabs v-model="tab" @tab-change="onTabChange">
        <el-tab-pane label="进行中" name="ACTIVE" />
        <el-tab-pane label="已完成" name="COMPLETED" />
        <el-tab-pane label="已取消" name="CANCELLED" />
        <el-tab-pane label="全部" name="ALL" />
      </el-tabs>

      <div v-loading="loading">
        <el-empty v-if="!list.length" :description="emptyText">
          <el-button type="primary" @click="$router.push('/tasks')">去任务广场看看</el-button>
        </el-empty>

        <div v-for="o in list" :key="o.id" class="order">
          <div class="order-head">
            <div class="title-wrap">
              <el-icon class="cat-icon"><component :is="categoryIcon(o.category)" /></el-icon>
              <span class="title" @click="$router.push(`/task/${o.taskId}`)">{{ o.title }}</span>
            </div>
            <div class="head-right">
              <el-tag :type="statusType(o.status)" size="small">{{ statusLabel(o.status) }}</el-tag>
              <b class="reward">¥{{ o.reward }}</b>
            </div>
          </div>

          <div class="order-body">
            <div class="addr">
              <div><el-icon><Location /></el-icon> 取件：{{ o.pickupAddress }}</div>
              <div><el-icon><Position /></el-icon> 送达：{{ o.deliveryAddress }}</div>
            </div>
            <div class="side">
              <div class="side-item">
                <span class="k">发布者</span>
                <span class="v">{{ o.publisherName || '—' }}</span>
              </div>
              <div class="side-item">
                <span class="k">接单时间</span>
                <span class="v">{{ o.createTime || '—' }}</span>
              </div>
              <div v-if="o.deadline" class="side-item">
                <span class="k">截止</span>
                <span class="v" :class="{ urgent: isUrgent(o.deadline) && !isDone(o.status) }">
                  {{ o.deadline }}（{{ countdownText(o.deadline) }}）
                </span>
              </div>
            </div>
          </div>

          <el-progress
            :percentage="(statusStep(o.status) / 4) * 100"
            :show-text="false"
            :stroke-width="5"
            :color="progressColor"
            class="progress"
          />

          <div class="order-foot">
            <el-button size="small" @click="$router.push(`/task/${o.taskId}`)">查看详情</el-button>
            <el-button v-if="o.status === 'ACCEPTED'" size="small" type="primary" @click="doStart(o)">开始配送</el-button>
            <el-button v-if="o.status === 'IN_PROGRESS'" size="small" type="success" @click="doFinish(o)">标记送达</el-button>
            <el-button
              v-if="['ACCEPTED', 'IN_PROGRESS'].includes(o.status)"
              size="small"
              type="danger"
              plain
              @click="doCancel(o)"
            >取消接单</el-button>
          </div>
        </div>
      </div>

      <div v-if="total > 0" class="pager">
        <el-pagination
          layout="prev, pager, next, total"
          :total="total"
          :page-size="query.size"
          :current-page="query.page"
          @current-change="onPage"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myOrders, startTask, finishTask, cancelTask, taskOverview } from '../api/task'
import {
  categoryIcon,
  countdownText,
  isUrgent,
  statusLabel,
  statusStep,
  statusType
} from '../utils/status'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const tab = ref('ACTIVE')
const query = reactive({ page: 1, size: 10 })
const overview = reactive({ activeCount: 0, completedCount: 0, cancelledCount: 0 })

const progressColor = [
  { color: '#f0a020', percentage: 30 },
  { color: '#409eff', percentage: 60 },
  { color: '#67c23a', percentage: 100 }
]

const summary = computed(() => [
  { key: 'ACTIVE', label: '进行中', value: overview.activeCount, color: '#409eff' },
  { key: 'COMPLETED', label: '已完成', value: overview.completedCount, color: '#67c23a' },
  { key: 'CANCELLED', label: '已取消', value: overview.cancelledCount, color: '#909399' }
])

const emptyText = computed(() =>
  ({ ACTIVE: '当前没有进行中的接单', COMPLETED: '还没有完成的接单', CANCELLED: '没有已取消的接单', ALL: '还没有接过单' }[tab.value])
)

const isDone = (s) => ['COMPLETED', 'CANCELLED'].includes(s)

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (tab.value !== 'ALL') params.status = tab.value
    const data = await myOrders(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadOverview() {
  Object.assign(overview, await taskOverview())
}

function switchTab(key) {
  tab.value = key
  query.page = 1
  load()
}

function onTabChange() {
  query.page = 1
  load()
}

function onPage(p) {
  query.page = p
  load()
}

async function doStart(o) {
  await startTask(o.taskId)
  ElMessage.success('已开始配送')
  load()
  loadOverview()
}

async function doFinish(o) {
  await finishTask(o.taskId)
  ElMessage.success('已标记送达，等待发布者确认')
  load()
  loadOverview()
}

async function doCancel(o) {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因（选填）', '取消接单', {
      inputPlaceholder: '原因',
      confirmButtonText: '确认取消',
      type: 'warning'
    })
    await cancelTask(o.taskId, value || '')
    ElMessage.success('已取消接单')
    load()
    loadOverview()
  } catch {
    /* 放弃 */
  }
}

onMounted(() => {
  load()
  loadOverview()
})
</script>

<style scoped>
.summary {
  margin-bottom: 16px;
}
.sum-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}
.sum-card.active {
  border-color: #5b6bf5;
  box-shadow: 0 0 0 1px #5b6bf5 inset;
}
.sum-value {
  font-size: 24px;
  font-weight: 700;
}
.sum-label {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.order {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 12px;
  transition: box-shadow 0.2s;
}
.order:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
}
.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.cat-icon {
  color: #5b6bf5;
}
.title {
  font-weight: 600;
  color: #303133;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.title:hover {
  color: #5b6bf5;
}
.head-right {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}
.reward {
  color: #f56c6c;
  font-size: 17px;
}
.order-body {
  display: flex;
  gap: 20px;
  margin: 10px 0 6px;
}
.addr {
  flex: 1;
  font-size: 13px;
  color: #606266;
  line-height: 1.9;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 8px 12px;
}
.side {
  width: 260px;
  font-size: 12px;
}
.side-item {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
}
.side .k {
  color: #909399;
}
.side .v {
  color: #606266;
  text-align: right;
}
.side .urgent {
  color: #f56c6c;
  font-weight: 600;
}
.progress {
  margin: 4px 0 8px;
}
.order-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
@media (max-width: 768px) {
  .order-body {
    flex-direction: column;
  }
  .side {
    width: 100%;
  }
}
</style>
