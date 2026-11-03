<template>
  <div>
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="搜索标题/描述" clearable style="width: 200px" @keyup.enter="onSearch" @clear="onSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.category" clearable placeholder="全部" style="width: 130px" @change="onSearch">
            <el-option v-for="(v, k) in CATEGORY" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 130px" @change="onSearch">
            <el-option v-for="(v, k) in TASK_STATUS" :key="k" :label="v.label" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col v-for="t in list" :key="t.id" :xs="24" :sm="12" :md="8" style="margin-bottom: 16px">
        <el-card shadow="hover" class="task-card">
          <div class="card-head">
            <el-tag :type="statusType(t.status)" size="small">{{ statusLabel(t.status) }}</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ categoryLabel(t.category) }}</el-tag>
            <span class="reward">¥{{ t.reward }}</span>
          </div>
          <h3 class="t-title">{{ t.title }}</h3>
          <p class="t-desc">{{ t.description || '暂无描述' }}</p>
          <div class="t-addr">
            <div>取：{{ t.pickupAddress }}</div>
            <div>送：{{ t.deliveryAddress }}</div>
          </div>
          <div class="t-meta">
            <span>发布：{{ t.publisherName }}</span>
            <span v-if="t.deadline">截止：{{ t.deadline }}</span>
          </div>
          <div class="card-foot">
            <el-button size="small" @click="goDetail(t.id)">查看详情</el-button>
            <el-button
              v-if="t.status === 'PENDING' && t.myRole === 'NONE'"
              size="small"
              type="primary"
              @click="accept(t)"
            >立即接单</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!list.length" description="暂无符合条件的任务" />
    <div v-if="total > 0" class="pager">
      <el-pagination
        layout="prev, pager, next, total"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { queryTasks, acceptTask } from '../api/task'
import { TASK_STATUS, CATEGORY, statusLabel, statusType, categoryLabel } from '../utils/status'

const router = useRouter()
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 9, keyword: '', category: '', status: '' })

async function load() {
  const params = { page: query.page, size: query.size }
  if (query.keyword) params.keyword = query.keyword
  if (query.category) params.category = query.category
  if (query.status) params.status = query.status
  const data = await queryTasks(params)
  list.value = data.records
  total.value = data.total
}

function onSearch() {
  query.page = 1
  load()
}

function goDetail(id) {
  router.push(`/task/${id}`)
}

async function accept(t) {
  try {
    await ElMessageBox.confirm(`确认接单「${t.title}」（报酬 ¥${t.reward}）？`, '接单确认', { type: 'warning' })
  } catch {
    return
  }
  await acceptTask(t.id)
  ElMessage.success('接单成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}
.task-card {
  height: 100%;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.reward {
  margin-left: auto;
  color: #f56c6c;
  font-weight: 700;
  font-size: 18px;
}
.t-title {
  margin: 10px 0 6px;
  font-size: 16px;
  color: #303133;
}
.t-desc {
  color: #909399;
  font-size: 13px;
  min-height: 38px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.t-addr {
  margin: 8px 0;
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
  background: #f5f7fa;
  padding: 6px 10px;
  border-radius: 6px;
}
.t-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}
.card-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
