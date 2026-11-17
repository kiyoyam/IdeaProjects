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
        <div class="eval-body">
          <div v-if="e.tags && e.tags.length" class="eval-tags">
            <el-tag v-for="t in e.tags" :key="t" size="small" effect="plain">{{ t }}</el-tag>
          </div>
          <span class="content">{{ e.content || '（未填写文字评价）' }}</span>
        </div>
        <span class="time">{{ e.createTime }}</span>
      </div>
      <el-button v-if="canEvaluate" type="primary" plain style="margin-top: 12px" @click="openEval">我要评价</el-button>
      <span v-else-if="task.status === 'COMPLETED' && task.myRole !== 'NONE'" class="evaluated-tip">你已完成评价，感谢反馈</span>
    </el-card>

    <RateDialog
      v-model="dialog"
      :task-id="taskId"
      :target-name="evalTargetName"
      :role="task?.myRole === 'PUBLISHER' ? 'PUBLISHER' : 'RUNNER'"
      @success="load"
    />
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskDetail, acceptTask, startTask, finishTask, confirmTask, cancelTask } from '../api/task'
import { taskEvaluations } from '../api/evaluation'
import RateDialog from '../components/RateDialog.vue'
import { PROGRESS_STEPS, statusStep, statusLabel, statusType, categoryLabel } from '../utils/status'

const route = useRoute()
const router = useRouter()
const taskId = route.params.id
const task = ref(null)
const evaluations = ref([])
const dialog = ref(false)

const canEvaluate = computed(() =>
  task.value && task.value.status === 'COMPLETED' && task.value.myRole !== 'NONE' && !task.value.evaluated
)

/** 评价对象：我是发布者就评跑腿员，反之评发布者 */
const evalTargetName = computed(() => {
  if (!task.value) return ''
  return task.value.myRole === 'PUBLISHER' ? task.value.runnerName : task.value.publisherName
})

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
.eval-body {
  flex: 1;
  min-width: 0;
}
.eval-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.evaluated-tip {
  display: inline-block;
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
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
