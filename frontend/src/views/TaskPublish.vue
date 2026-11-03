<template>
  <el-card shadow="never" class="publish-card">
    <h3>发布任务</h3>
    <el-form :model="form" label-width="90px">
      <el-form-item label="任务标题" required>
        <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="一句话说清楚要做什么" />
      </el-form-item>
      <el-form-item label="任务分类">
        <el-select v-model="form.category" style="width: 200px">
          <el-option v-for="(v, k) in CATEGORY" :key="k" :label="v" :value="k" />
        </el-select>
      </el-form-item>
      <el-form-item label="报酬金额" required>
        <el-input-number v-model="form.reward" :min="0.01" :precision="2" :step="1" />
        <span class="tip">发布时冻结该金额，任务完成结算给跑腿员，取消则原路退回</span>
      </el-form-item>
      <el-form-item label="取件地址" required>
        <el-input v-model="form.pickupAddress" placeholder="如：菜鸟驿站 / 食堂二楼" />
      </el-form-item>
      <el-form-item label="送达地址" required>
        <el-input v-model="form.deliveryAddress" placeholder="如：5栋宿舍楼下 / 图书馆一楼" />
      </el-form-item>
      <el-form-item label="截止时间">
        <el-date-picker
          v-model="form.deadline"
          type="datetime"
          placeholder="选填"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 220px"
        />
      </el-form-item>
      <el-form-item label="任务描述">
        <el-input v-model="form.description" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="补充任务细节，如物品大小、联系方式、注意事项等" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">发布任务</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { publishTask } from '../api/task'
import { CATEGORY } from '../utils/status'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  title: '',
  category: 'PICKUP',
  reward: 5,
  pickupAddress: '',
  deliveryAddress: '',
  deadline: '',
  description: ''
})

async function submit() {
  if (!form.title) return ElMessage.warning('请填写任务标题')
  if (!form.pickupAddress) return ElMessage.warning('请填写取件地址')
  if (!form.deliveryAddress) return ElMessage.warning('请填写送达地址')
  if (!form.reward || form.reward <= 0) return ElMessage.warning('请填写正确的报酬金额')
  loading.value = true
  try {
    const data = await publishTask({ ...form })
    ElMessage.success('发布成功，报酬已冻结')
    router.push(`/task/${data.id}`)
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(form, {
    title: '',
    category: 'PICKUP',
    reward: 5,
    pickupAddress: '',
    deliveryAddress: '',
    deadline: '',
    description: ''
  })
}
</script>

<style scoped>
.publish-card {
  max-width: 760px;
  margin: 0 auto;
}
.publish-card h3 {
  margin-bottom: 20px;
  color: #303133;
}
.tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
