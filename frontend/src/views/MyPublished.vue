<template>
  <el-card shadow="never">
    <h3>我发布的任务</h3>
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="报酬" width="90">
        <template #default="{ row }"><b class="reward">¥{{ row.reward }}</b></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="跑腿员" width="120">
        <template #default="{ row }">{{ row.runnerName || '待接单' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="170" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="goDetail(row.id)">详情</el-button>
          <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="doCancel(row)">取消</el-button>
          <el-button v-if="row.status === 'FINISHED'" size="small" type="success" @click="doConfirm(row)">确认完成</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!list.length" description="还没有发布过任务" />
    <div v-if="total > 0" class="pager">
      <el-pagination layout="prev, pager, next, total" :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />
    </div>
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myPublished, cancelTask, confirmTask } from '../api/task'
import { statusLabel, statusType } from '../utils/status'

const router = useRouter()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const data = await myPublished({ page: query.page, size: query.size })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/task/${id}`)
}

async function doCancel(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因（选填）', '取消任务', { inputPlaceholder: '原因', confirmButtonText: '确认取消', type: 'warning' })
    await cancelTask(row.id, value || '')
    ElMessage.success('已取消，冻结金额已退回')
    load()
  } catch {
    /* 放弃 */
  }
}

async function doConfirm(row) {
  try {
    await ElMessageBox.confirm('确认任务已完成？确认后报酬将结算给跑腿员。', '确认完成', { type: 'warning' })
  } catch {
    return
  }
  await confirmTask(row.id)
  ElMessage.success('已确认，报酬已结算给跑腿员')
  load()
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
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
