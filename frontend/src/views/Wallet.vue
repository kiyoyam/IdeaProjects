<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="c in statCards" :key="c.label" :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">{{ c.label }}</div>
          <div class="stat-value" :style="{ color: c.color }">{{ c.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <div class="toolbar">
        <h3>资金明细</h3>
        <div>
          <el-button type="primary" plain @click="rechargeDialog = true">充值</el-button>
          <el-button type="success" plain @click="withdrawDialog = true">提现</el-button>
        </div>
      </div>
      <el-tabs v-model="tab">
        <el-tab-pane label="结算记录" name="settle">
          <el-table :data="settleList" stripe>
            <el-table-column prop="title" label="任务" min-width="160" show-overflow-tooltip />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.type === 'TASK' ? 'success' : 'info'" size="small">{{ row.type === 'TASK' ? '任务结算' : '退款' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="100">
              <template #default="{ row }">
                <b :style="{ color: row.type === 'TASK' ? '#67c23a' : '#909399' }">
                  {{ row.type === 'TASK' ? '+' : '-' }}¥{{ row.amount }}
                </b>
              </template>
            </el-table-column>
            <el-table-column label="对方" width="120">
              <template #default="{ row }">
                {{ row.publisherId === store.user?.id ? `付给：${row.runnerName}` : `来自：${row.publisherName}` }}
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="170" />
          </el-table>
          <div v-if="settleTotal > 0" class="pager">
            <el-pagination layout="prev, pager, next" :total="settleTotal" :page-size="10" :current-page="settleQuery.page" @current-change="loadSettles" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="提现记录" name="withdraw">
          <el-table :data="withdrawList" stripe>
            <el-table-column label="金额" width="120">
              <template #default="{ row }"><b>¥{{ row.amount }}</b></template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="withdrawType(row.status)" size="small">{{ withdrawLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            <el-table-column prop="createTime" label="申请时间" width="170" />
            <el-table-column prop="handleTime" label="处理时间" width="170">
              <template #default="{ row }">{{ row.handleTime || '—' }}</template>
            </el-table-column>
          </el-table>
          <div v-if="withdrawTotal > 0" class="pager">
            <el-pagination layout="prev, pager, next" :total="withdrawTotal" :page-size="10" :current-page="withdrawQuery.page" @current-change="loadWithdraws" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="rechargeDialog" title="充值" width="400px">
      <el-form label-width="80px">
        <el-form-item label="充值金额">
          <el-input-number v-model="rechargeAmount" :min="1" :precision="2" :step="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialog = false">取消</el-button>
        <el-button type="primary" :loading="rechargeLoading" @click="doRecharge">确认充值</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="withdrawDialog" title="申请提现" width="400px">
      <el-form label-width="80px">
        <el-form-item label="提现金额">
          <el-input-number v-model="withdrawAmount" :min="1" :max="Number(wallet.balance || 0)" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item>
          <span class="tip">可提现余额 ¥{{ wallet.balance ?? '--' }}，提现需管理员审核</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="withdrawDialog = false">取消</el-button>
        <el-button type="primary" :loading="withdrawLoading" @click="doWithdraw">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { walletInfo, mySettlements, myWithdrawals, withdraw } from '../api/settlement'
import { recharge } from '../api/auth'
import { useUserStore } from '../store/user'
import { withdrawLabel, withdrawType } from '../utils/status'

const store = useUserStore()
const wallet = ref({})
const settleList = ref([])
const settleTotal = ref(0)
const settleQuery = reactive({ page: 1, size: 10 })
const withdrawList = ref([])
const withdrawTotal = ref(0)
const withdrawQuery = reactive({ page: 1, size: 10 })
const tab = ref('settle')
const rechargeDialog = ref(false)
const withdrawDialog = ref(false)
const rechargeAmount = ref(50)
const withdrawAmount = ref(10)
const rechargeLoading = ref(false)
const withdrawLoading = ref(false)

const statCards = computed(() => [
  { label: '可用余额', value: `¥${wallet.value.balance ?? '--'}`, color: '#67c23a' },
  { label: '冻结中', value: `¥${wallet.value.frozenBalance ?? '--'}`, color: '#e6a23c' },
  { label: '累计收入', value: `¥${wallet.value.totalIncome ?? '--'}`, color: '#409eff' },
  { label: '累计支出', value: `¥${wallet.value.totalExpense ?? '--'}`, color: '#f56c6c' },
  { label: '累计提现', value: `¥${wallet.value.totalWithdrawn ?? '--'}`, color: '#909399' },
  { label: '完成接单', value: `${wallet.value.completedOrders ?? '--'} 单`, color: '#303133' }
])

async function loadAll() {
  wallet.value = await walletInfo()
  loadSettles()
  loadWithdraws()
}

async function loadSettles() {
  const data = await mySettlements({ page: settleQuery.page, size: settleQuery.size })
  settleList.value = data.records
  settleTotal.value = data.total
}

async function loadWithdraws() {
  const data = await myWithdrawals({ page: withdrawQuery.page, size: withdrawQuery.size })
  withdrawList.value = data.records
  withdrawTotal.value = data.total
}

async function doRecharge() {
  rechargeLoading.value = true
  try {
    const user = await recharge({ amount: rechargeAmount.value })
    store.user = user
    ElMessage.success('充值成功')
    rechargeDialog.value = false
    loadAll()
  } finally {
    rechargeLoading.value = false
  }
}

async function doWithdraw() {
  if (!withdrawAmount.value || withdrawAmount.value <= 0) return ElMessage.warning('请输入正确的提现金额')
  withdrawLoading.value = true
  try {
    await withdraw({ amount: withdrawAmount.value })
    ElMessage.success('提现申请已提交，等待管理员审核')
    withdrawDialog.value = false
    loadAll()
  } finally {
    withdrawLoading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.stat-card {
  text-align: center;
  margin-bottom: 16px;
}
.stat-label {
  color: #909399;
  font-size: 13px;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  margin-top: 6px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.tip {
  color: #909399;
  font-size: 12px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
