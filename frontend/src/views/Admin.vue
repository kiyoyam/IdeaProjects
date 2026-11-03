<template>
  <el-card shadow="never">
    <h3>管理后台</h3>
    <el-tabs v-model="tab">
      <el-tab-pane label="用户管理" name="users">
        <div class="toolbar">
          <el-input v-model="userKeyword" placeholder="搜索用户名/昵称" clearable style="width: 240px" @keyup.enter="loadUsers" />
          <el-button type="primary" @click="loadUsers">查询</el-button>
        </div>
        <el-table :data="users" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="账号" width="130" />
          <el-table-column prop="nickname" label="昵称" width="130" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }"><el-tag size="small">{{ roleLabel(row.role) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="余额" width="100">
            <template #default="{ row }">¥{{ row.balance }}</template>
          </el-table-column>
          <el-table-column label="冻结" width="100">
            <template #default="{ row }">¥{{ row.frozenBalance }}</template>
          </el-table-column>
          <el-table-column label="评分" width="140">
            <template #default="{ row }">
              <el-rate :model-value="row.rating" disabled size="small" />
              <span class="cnt">({{ row.ratingCount }})</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="注册时间" width="170" />
        </el-table>
        <div v-if="userTotal > 0" class="pager">
          <el-pagination layout="prev, pager, next" :total="userTotal" :page-size="10" :current-page="userPage" @current-change="(p) => { userPage = p; loadUsers() }" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="任务管理" name="tasks">
        <el-table :data="tasks" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
          <el-table-column label="报酬" width="90">
            <template #default="{ row }">¥{{ row.reward }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="publisherName" label="发布者" width="110" />
          <el-table-column prop="runnerName" label="跑腿员" width="110">
            <template #default="{ row }">{{ row.runnerName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="发布时间" width="170" />
        </el-table>
        <div v-if="taskTotal > 0" class="pager">
          <el-pagination layout="prev, pager, next" :total="taskTotal" :page-size="10" :current-page="taskPage" @current-change="(p) => { taskPage = p; loadTasks() }" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="结算记录" name="settles">
        <el-table :data="settles" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="任务" min-width="150" show-overflow-tooltip />
          <el-table-column label="类型" width="100">
            <template #default="{ row }"><el-tag :type="row.type === 'TASK' ? 'success' : 'info'" size="small">{{ row.type === 'TASK' ? '任务结算' : '退款' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="金额" width="90">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column prop="publisherName" label="发布者" width="110" />
          <el-table-column prop="runnerName" label="跑腿员" width="110" />
          <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
          <el-table-column prop="createTime" label="时间" width="170" />
        </el-table>
        <div v-if="settleTotal > 0" class="pager">
          <el-pagination layout="prev, pager, next" :total="settleTotal" :page-size="10" :current-page="settlePage" @current-change="(p) => { settlePage = p; loadSettles() }" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="提现审批" name="withdraws">
        <el-table :data="withdraws" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="username" label="账号" width="120" />
          <el-table-column prop="nickname" label="昵称" width="120" />
          <el-table-column label="金额" width="100">
            <template #default="{ row }"><b>¥{{ row.amount }}</b></template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><el-tag :type="withdrawType(row.status)" size="small">{{ withdrawLabel(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
          <el-table-column prop="createTime" label="申请时间" width="170" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button size="small" type="success" @click="approve(row)">通过</el-button>
                <el-button size="small" type="danger" @click="reject(row)">驳回</el-button>
              </template>
              <span v-else class="done">已处理</span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="withdrawTotal > 0" class="pager">
          <el-pagination layout="prev, pager, next" :total="withdrawTotal" :page-size="10" :current-page="withdrawPage" @current-change="(p) => { withdrawPage = p; loadWithdraws() }" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminUsers, adminTasks, adminSettlements, adminWithdrawals, approveWithdrawal, rejectWithdrawal } from '../api/admin'
import { roleLabel, statusLabel, statusType, withdrawLabel, withdrawType } from '../utils/status'

const tab = ref('users')
const users = ref([])
const userTotal = ref(0)
const userPage = ref(1)
const userKeyword = ref('')
const tasks = ref([])
const taskTotal = ref(0)
const taskPage = ref(1)
const settles = ref([])
const settleTotal = ref(0)
const settlePage = ref(1)
const withdraws = ref([])
const withdrawTotal = ref(0)
const withdrawPage = ref(1)

async function loadUsers() {
  const data = await adminUsers({ page: userPage.value, size: 10, keyword: userKeyword.value || undefined })
  users.value = data.records
  userTotal.value = data.total
}

async function loadTasks() {
  const data = await adminTasks({ page: taskPage.value, size: 10 })
  tasks.value = data.records
  taskTotal.value = data.total
}

async function loadSettles() {
  const data = await adminSettlements({ page: settlePage.value, size: 10 })
  settles.value = data.records
  settleTotal.value = data.total
}

async function loadWithdraws() {
  const data = await adminWithdrawals({ page: withdrawPage.value, size: 10 })
  withdraws.value = data.records
  withdrawTotal.value = data.total
}

async function approve(row) {
  try {
    await ElMessageBox.confirm(`确认通过用户「${row.username}」的 ¥${row.amount} 提现申请？`, '审批确认', { type: 'warning' })
  } catch {
    return
  }
  await approveWithdrawal(row.id)
  ElMessage.success('已通过')
  loadWithdraws()
}

async function reject(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因（选填）', '驳回提现', { inputPlaceholder: '原因', confirmButtonText: '确认驳回', type: 'warning' })
    await rejectWithdrawal(row.id, value || '')
    ElMessage.success('已驳回，金额已退回用户余额')
    loadWithdraws()
  } catch {
    /* 放弃 */
  }
}

onMounted(() => {
  loadUsers()
  loadTasks()
  loadSettles()
  loadWithdraws()
})
</script>

<style scoped>
h3 {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
.cnt {
  color: #909399;
  font-size: 12px;
}
.done {
  color: #c0c4cc;
  font-size: 13px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
