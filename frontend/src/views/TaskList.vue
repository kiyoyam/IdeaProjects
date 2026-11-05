<template>
  <div>
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="搜索标题/描述"
            clearable
            style="width: 200px"
            @keyup.enter="onSearch"
            @clear="onSearch"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.category" clearable placeholder="全部" style="width: 130px" @change="onSearch">
            <el-option v-for="(v, k) in CATEGORY" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="query.sort" style="width: 130px" @change="onSearch">
            <el-option v-for="s in SORT_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="chips">
        <span class="chips-label">快捷筛选：</span>
        <el-check-tag :checked="query.status === 'PENDING'" @change="quickStatus('PENDING')">只看待接单</el-check-tag>
        <el-check-tag :checked="query.status === 'IN_PROGRESS'" @change="quickStatus('IN_PROGRESS')">配送中</el-check-tag>
        <el-check-tag :checked="query.category === 'BUY'" @change="quickCategory('BUY')">代买</el-check-tag>
        <el-check-tag :checked="query.category === 'PICKUP'" @change="quickCategory('PICKUP')">取件</el-check-tag>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col v-for="t in list" :key="t.id" :xs="24" :sm="12" :md="8" style="margin-bottom: 16px">
        <el-card shadow="hover" class="task-card">
          <div class="card-head">
            <el-tag :type="statusType(t.status)" size="small">{{ statusLabel(t.status) }}</el-tag>
            <el-tag size="small" type="info" effect="plain">
              <el-icon class="tag-icon"><component :is="categoryIcon(t.category)" /></el-icon>
              {{ categoryLabel(t.category) }}
            </el-tag>
            <el-tag v-if="isUrgent(t.deadline) && t.status === 'PENDING'" size="small" type="danger" effect="dark">加急</el-tag>
            <span class="reward">¥{{ t.reward }}</span>
          </div>
          <h3 class="t-title" @click="goDetail(t.id)">{{ t.title }}</h3>
          <p class="t-desc">{{ t.description || '暂无描述' }}</p>
          <div class="t-addr">
            <div><el-icon><Location /></el-icon> 取：{{ t.pickupAddress }}</div>
            <div><el-icon><Position /></el-icon> 送：{{ t.deliveryAddress }}</div>
          </div>
          <div class="t-meta">
            <span>发布：{{ t.publisherName }}</span>
            <span v-if="t.deadline" :class="{ urgent: isUrgent(t.deadline) }">
              {{ countdownText(t.deadline) }}
            </span>
          </div>
          <div class="card-foot">
            <el-button size="small" @click="goDetail(t.id)">查看详情</el-button>
            <el-button
              v-if="t.status === 'PENDING' && t.myRole === 'NONE'"
              size="small"
              type="primary"
              @click="accept(t)"
            >立即接单</el-button>
            <el-button v-else-if="t.myRole === 'RUNNER'" size="small" type="success" plain @click="goAccepted">
              我的接单
            </el-button>
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
        @current-change="onPage"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { queryTasks, acceptTask } from '../api/task'
import {
  CATEGORY,
  SORT_OPTIONS,
  categoryIcon,
  categoryLabel,
  countdownText,
  isUrgent,
  statusLabel,
  statusType
} from '../utils/status'

const router = useRouter()
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 9, keyword: '', category: '', status: '', sort: 'latest' })

async function load() {
  const params = { page: query.page, size: query.size, sort: query.sort }
  if (query.keyword) params.keyword = query.keyword
  if (query.category) params.category = query.category
  if (query.status) params.status = query.status
  const data = await queryTasks(params)
  list.value = data.records || []
  total.value = data.total || 0
}

function onSearch() {
  query.page = 1
  load()
}

function onPage(p) {
  query.page = p
  load()
}

function resetQuery() {
  query.keyword = ''
  query.category = ''
  query.status = ''
  query.sort = 'latest'
  onSearch()
}

function quickStatus(s) {
  query.status = query.status === s ? '' : s
  onSearch()
}

function quickCategory(c) {
  query.category = query.category === c ? '' : c
  onSearch()
}

function goDetail(id) {
  router.push(`/task/${id}`)
}

function goAccepted() {
  router.push('/my/accepted')
}

async function accept(t) {
  try {
    await ElMessageBox.confirm(
      `确认接单「${t.title}」（报酬 ¥${t.reward}）？接单后请按时完成配送。`,
      '接单确认',
      { type: 'warning', confirmButtonText: '确认接单' }
    )
  } catch {
    return
  }
  await acceptTask(t.id)
  load()
  // 接单成功后可直接跳到自己手上的单
  try {
    await ElMessageBox.confirm('接单成功，是否立即查看你的接单？', '接单成功', {
      type: 'success',
      confirmButtonText: '查看我的接单',
      cancelButtonText: '继续逛广场'
    })
    router.push('/my/accepted')
  } catch {
    /* 留在广场 */
  }
}

onMounted(load)
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}
.chips {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.chips-label {
  font-size: 13px;
  color: #909399;
}
.task-card {
  height: 100%;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tag-icon {
  vertical-align: -2px;
  margin-right: 2px;
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
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.t-title:hover {
  color: #5b6bf5;
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
  line-height: 1.7;
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
.t-meta .urgent {
  color: #f56c6c;
  font-weight: 600;
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
