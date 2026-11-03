<template>
  <el-card shadow="never">
    <h3>我接的订单</h3>
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="title" label="任务标题" min-width="160" show-overflow-tooltip />
      <el-table-column label="报酬" width="90">
        <template #default="{ row }"><b class="reward">¥{{ row.reward }}</b></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布者" width="110">
        <template #default="{ row }">{{ row.publisherName }}</template>
      </el-table-column>
      <el-table-column label="取件/送达" min-width="220">
        <template #default="{ row }">
          <div class="addr">取：{{ row.pickupAddress }}</div>
          <div class="addr">送：{{ row.deliveryAddress }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="接单时间" width="170" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="goDetail(row.taskId)">详情</el-button>
          <el-button v-if="row.status === 'ACCEPTED'" size="small" type="primary" @click="doStart(row)">开始配送</el-button>
          <el-button v-if="row.status === 'IN_PROGRESS'" size="small" type="success" @click="doFinish(row)">标记送达</el-button>
          <el-button v-if="['ACCEPTED', 'IN_PROGRESS'].includes(row.status)" size="small" type="danger" @click="doCancel(row)">取消接单</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!list.length" description="还没有接过单，去任务广场看看吧" />
    <div v-if="total > 0" class="pager">
      <el-pagination layout="prev, pager, next, total" :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />
    </div>
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myOrders, startTask, finishTask, cancelTask } from '../api/task'
import { statusLabel, statusType } from '../utils/status'

const router = useRouter()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const data = await myOrders({ page: query.page, size: query.size })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function goDetail(taskId) {
  router.push(`/task/${taskId}`)
}

async function doStart(row) {
  await startTask(row.taskId)
  ElMessage.success('已开始配送')
  load()
}

async function doFinish(row) {
  await finishTask(row.taskId)
  ElMessage.success('已标记送达，等待发布者确认')
  load()
}

async function doCancel(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因（选填）', '取消接单', { inputPlaceholder: '原因', confirmButtonText: '确认取消', type: 'warning' })
    await cancelTask(row.taskId, value || '')
    ElMessage.success('已取消接单')
    load()
  } catch {
    /* 放弃 */
  }
}

onMounted(load)
</script>

<style scoped>
h3 {
  margin-bottom: 16px;
}
.reward {
  color: #f56c6c;
}
.addr {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
