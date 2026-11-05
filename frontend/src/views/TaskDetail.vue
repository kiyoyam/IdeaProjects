<template>
  <div v-if="task">
    <el-card shadow="never">
      <div class="head">
        <h2>{{ task.title }}</h2>
        <div>
          <el-tag :type="statusType(task.status)">{{ statusLabel(task.status) }}</el-tag>
          <el-tag type="info" effect="plain" style="margin-left: 8px">{{ categoryLabel(task.category) }}</el-tag>
        </div>
      </div>
      <el-steps v-if="task.status !== 'CANCELLED'" :active="statusStep(task.status)" align-center class="steps">
        <el-step v-for="s in PROGRESS_STEPS" :key="s" :title="s" />
      </el-steps>
      <el-alert
        v-else
        :title="`任务已取消${task.cancelReason ? '：' + task.cancelReason : ''}`"
        type="warning"
        :closable="false"
        class="steps"
      />

      <el-descriptions :column="2" border>
        <el-descriptions-item label="报酬">
          <b class="reward">¥{{ task.reward }}</b>
        </el-descriptions-item>
        <el-descriptions-item label="发布者">
          {{ task.publisherName }}
          <el-rate v-if="task.publisherRating" :model-value="task.publisherRating" disabled size="small" style="display: inline-block; margin-left: 8px" />
        </el-descriptions-item>
        <el-descriptions-item label="取件地址">{{ task.pickupAddress }}</el-descriptions-item>
        <el-descriptions-item label="送达地址">{{ task.deliveryAddress }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ task.deadline || '无' }}</el-descriptions-item>
        <el-descriptions-item label="跑腿员">
          {{ task.runnerName || '待接单' }}
          <el-rate v-if="task.runnerRating" :model-value="Number(task.runnerRating)" disabled size="small" style="display: inline-block; margin-left: 8px" />
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ task.createTime }}</el-descriptions-item>
        <el-descriptions-item label="接单时间">{{ task.acceptTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ task.completeTime || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="task.cancelReason" label="取消原因" :span="2">{{ task.cancelReason }}</el-descriptions-item>
        <el-descriptions-item label="任务描述" :span="2">{{ task.description || '暂无' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="ops.length" class="ops">
        <el-button v-for="op in ops" :key="op.action" :type="op.type" @click="doOp(op)">{{ op.label }}</el-button>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <h3>任务评价</h3>
      <div v-if="!evaluations.length" class="empty">暂无评价</div>
      <div v-for="e in evaluations" :key="e.id" class="eval-item">
        <span class="name">{{ e.evaluatorName }}</span>
        <el-rate :model-value="e.rating" disabled />
        <span class="content">{{ e.content }}</span>
        <span class="time">{{ e.createTime }}</span>
      </div>
      <el-button v-if="canEvaluate" type="primary" plain style="margin-top: 12px" @click="openEval">我要评价</el-button>
    </el-card>

    <el-dialog v-model="dialog" title="评价任务" width="440px">
      <el-form label-width="70px">
        <el-form-item label="评分">
          <el-rate v-model="evalForm.rating" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="evalForm.content" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="说说对方的服务体验（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="evalLoading" @click="submitEval">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskDetail, acceptTask, startTask, finishTask, confirmTask, cancelTask } from '../api/task'
import { taskEvaluations, evaluate } from '../api/evaluation'
import { PROGRESS_STEPS, statusStep, statusLabel, statusType, categoryLabel } from '../utils/status'

const route = useRoute()
const router = useRouter()
const taskId = route.params.id
const task = ref(null)
const evaluations = ref([])
const dialog = ref(false)
const evalLoading = ref(false)
const evalForm = reactive({ rating: 5, content: '' })

const canEvaluate = computed(() =>
  task.value && task.value.status === 'COMPLETED' && task.value.myRole !== 'NONE' && !task.value.evaluated
)

const ops = computed(() => {
  if (!task.value) return []
  const t = task.value
  const list = []
  if (t.status === 'PENDING' && t.myRole === 'NONE') list.push({ action: 'accept', label: '立即接单', type: 'primary' })
  if (t.status === 'PENDING' && t.myRole === 'PUBLISHER') list.push({ action: 'cancel', label: '取消任务', type: 'danger' })
  if (t.status === 'ACCEPTED' && t.myRole === 'RUNNER') list.push({ action: 'start', label: '开始配送', type: 'primary' })
  if (t.status === 'ACCEPTED' && t.myRole === 'PUBLISHER') list.push({ action: 'cancel', label: '取消任务', type: 'danger' })
  if (t.status === 'IN_PROGRESS' && t.myRole === 'RUNNER') {
    list.push({ action: 'finish', label: '标记送达', type: 'success' })
    list.push({ action: 'cancel', label: '取消接单', type: 'danger' })
  }
  if (t.status === 'IN_PROGRESS' && t.myRole === 'PUBLISHER') list.push({ action: 'cancel', label: '取消任务', type: 'danger' })
  if (t.status === 'FINISHED' && t.myRole === 'PUBLISHER') list.push({ action: 'confirm', label: '确认完成并结算', type: 'success' })
  return list
})

async function load() {
  task.value = await taskDetail(taskId)
  evaluations.value = await taskEvaluations(taskId)
}

async function doOp(op) {
  if (op.action === 'accept') {
    try {
      await ElMessageBox.confirm('确认接单？', '接单确认', { type: 'warning' })
    } catch {
      return
    }
  }
  if (op.action === 'cancel') {
    try {
      const { value } = await ElMessageBox.prompt('请输入取消原因（选填）', '取消任务', {
        inputPlaceholder: '原因',
        confirmButtonText: '确认取消',
        type: 'warning'
      })
      await cancelTask(taskId, value || '')
      ElMessage.success('任务已取消，冻结金额已退回')
      load()
    } catch {
      /* 用户放弃 */
    }
    return
  }
  const actions = { accept: acceptTask, start: startTask, finish: finishTask, confirm: confirmTask }
  await actions[op.action](taskId)
  if (op.action === 'accept') {
    load()
    try {
      await ElMessageBox.confirm('接单成功，是否立即查看你的接单？', '接单成功', {
        type: 'success',
        confirmButtonText: '查看我的接单',
        cancelButtonText: '留在此页'
      })
      router.push('/my/accepted')
    } catch {
      /* 留在详情页 */
    }
    return
  }
  ElMessage.success('操作成功')
  load()
}

function openEval() {
  dialog.value = true
  evalForm.rating = 5
  evalForm.content = ''
}

async function submitEval() {
  if (!evalForm.rating) return ElMessage.warning('请选择评分')
  evalLoading.value = true
  try {
    await evaluate({ taskId: Number(taskId), rating: evalForm.rating, content: evalForm.content })
    ElMessage.success('评价成功')
    dialog.value = false
    load()
  } finally {
    evalLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.head h2 {
  color: #303133;
}
.reward {
  color: #f56c6c;
  font-size: 20px;
}
.steps {
  margin-bottom: 18px;
}
.ops {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
.empty {
  color: #909399;
  padding: 12px 0;
}
.eval-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
}
.eval-item .name {
  font-weight: 600;
  color: #303133;
  min-width: 80px;
}
.eval-item .content {
  flex: 1;
  color: #606266;
  font-size: 14px;
}
.eval-item .time {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
