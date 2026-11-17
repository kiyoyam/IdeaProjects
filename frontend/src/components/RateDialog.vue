<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="560px"
    :close-on-click-modal="false"
    @update:model-value="(v) => emit('update:modelValue', v)"
    @open="reset"
  >
    <div class="dialog-head">
      <div class="who">
        评价对象：<b>{{ targetName || '对方' }}</b>
        <el-tag v-if="role === 'PUBLISHER'" size="small" type="warning" effect="plain" style="margin-left: 6px">跑腿员</el-tag>
        <el-tag v-else size="small" effect="plain" style="margin-left: 6px">发布者</el-tag>
      </div>
      <div class="tips">真实评价能帮到其他同学，提交后不可修改</div>
    </div>

    <!-- 总评分 -->
    <div class="block overall">
      <div class="overall-score">{{ form.rating.toFixed(1) }}</div>
      <div class="overall-main">
        <el-rate v-model="form.rating" :texts="RATE_TEXTS" show-text :allow-half="false" size="large" />
      </div>
    </div>

    <!-- 三维度 -->
    <div class="block">
      <div class="block-title">分项评分</div>
      <div v-for="d in DIMENSIONS" :key="d.key" class="dim-row">
        <span class="dim-label">{{ d.label }}</span>
        <el-rate v-model="form[d.key]" size="small" />
        <span class="dim-tip">{{ d.tip }}</span>
      </div>
    </div>

    <!-- 标签 -->
    <div class="block">
      <div class="block-title">
        快捷标签
        <span class="count">已选 {{ form.tags.length }}/6</span>
      </div>
      <div class="tags">
        <el-check-tag
          v-for="t in TAGS"
          :key="t"
          :checked="form.tags.includes(t)"
          @change="toggleTag(t)"
        >{{ t }}</el-check-tag>
      </div>
    </div>

    <!-- 文字 + 匿名 -->
    <div class="block">
      <div class="block-title">文字评价<span class="count">选填</span></div>
      <el-input
        v-model="form.content"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="说说这次体验，比如是否准时、沟通是否顺畅……"
      />
      <div class="anonymous-row">
        <span>匿名评价</span>
        <el-switch v-model="form.anonymous" />
        <span class="anonymous-tip">开启后对方看不到你的昵称</span>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交评价</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { evaluate } from '../api/evaluation'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  taskId: { type: [Number, String], default: null },
  targetName: { type: String, default: '' },
  /** PUBLISHER 表示我是发布者，正在评价跑腿员 */
  role: { type: String, default: 'PUBLISHER' },
  title: { type: String, default: '评价本次服务' }
})
const emit = defineEmits(['update:modelValue', 'success'])

const RATE_TEXTS = ['非常差', '较差', '一般', '满意', '非常满意']
const DIMENSIONS = [
  { key: 'speedRating', label: '配送速度', tip: '是否准时送达' },
  { key: 'serviceRating', label: '服务态度', tip: '沟通是否顺畅' },
  { key: 'qualityRating', label: '完成质量', tip: '物品是否完好' }
]
const TAGS = ['准时送达', '速度快', '态度好', '沟通顺畅', '包装完好', '礼貌耐心', '认真负责', '描述相符', '物超所值', '值得推荐']

const submitting = ref(false)
const form = reactive({
  rating: 5,
  speedRating: 5,
  serviceRating: 5,
  qualityRating: 5,
  content: '',
  tags: [],
  anonymous: false
})

const heading = computed(() => props.title)

function reset() {
  form.rating = 5
  form.speedRating = 5
  form.serviceRating = 5
  form.qualityRating = 5
  form.content = ''
  form.tags = []
  form.anonymous = false
}

function toggleTag(tag) {
  const idx = form.tags.indexOf(tag)
  if (idx >= 0) {
    form.tags.splice(idx, 1)
  } else if (form.tags.length >= 6) {
    ElMessage.warning('最多选择 6 个标签')
  } else {
    form.tags.push(tag)
  }
}

async function submit() {
  if (!form.rating) return ElMessage.warning('请先给出总评分')
  submitting.value = true
  try {
    await evaluate({
      taskId: Number(props.taskId),
      rating: form.rating,
      speedRating: form.speedRating,
      serviceRating: form.serviceRating,
      qualityRating: form.qualityRating,
      content: form.content,
      tags: form.tags,
      anonymous: form.anonymous
    })
    ElMessage.success('评价已提交，感谢你的反馈')
    emit('update:modelValue', false)
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.dialog-head {
  margin-bottom: 12px;
}
.who {
  font-size: 14px;
  color: #303133;
}
.tips {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.block {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 12px;
}
.block-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.count {
  font-size: 12px;
  font-weight: 400;
  color: #c0c4cc;
}
.overall {
  display: flex;
  align-items: center;
  gap: 16px;
  background: linear-gradient(120deg, #fff8ec, #fef6ff);
  border-color: #fbe3c0;
}
.overall-score {
  font-size: 34px;
  font-weight: 800;
  color: #f0a020;
  line-height: 1;
  min-width: 62px;
}
.overall-tip {
  font-size: 12px;
  color: #e6a23c;
  margin-top: 2px;
}
.dim-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 3px 0;
}
.dim-label {
  width: 68px;
  font-size: 13px;
  color: #606266;
}
.dim-tip {
  font-size: 12px;
  color: #c0c4cc;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.anonymous-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
}
.anonymous-tip {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
