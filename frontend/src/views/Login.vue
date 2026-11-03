<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">校园跑腿 · 任务发布与接单平台</h2>
      <el-tabs v-model="tab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-width="64px" @keyup.enter="doLogin">
            <el-form-item label="账号">
              <el-input v-model="loginForm.username" placeholder="请输入账号" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-button type="primary" style="width: 100%" :loading="loading" @click="doLogin">登 录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="regForm" label-width="64px" @keyup.enter="doRegister">
            <el-form-item label="账号">
              <el-input v-model="regForm.username" placeholder="3-20位" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="regForm.password" type="password" show-password placeholder="6-20位" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="regForm.nickname" placeholder="选填" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="regForm.phone" placeholder="选填" />
            </el-form-item>
            <el-form-item label="角色">
              <el-select v-model="regForm.role" style="width: 100%">
                <el-option label="学生（发布任务）" value="STUDENT" />
                <el-option label="跑腿员（接单赚钱）" value="RUNNER" />
              </el-select>
            </el-form-item>
            <el-button type="primary" style="width: 100%" :loading="loading" @click="doRegister">注 册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div class="tip">测试账号：stu001 / runner001 / admin，密码均为 123456</div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const router = useRouter()
const store = useUserStore()
const tab = ref('login')
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', nickname: '', phone: '', role: 'STUDENT' })

async function doLogin() {
  if (!loginForm.username || !loginForm.password) return ElMessage.warning('请输入账号和密码')
  loading.value = true
  try {
    await store.login(loginForm)
    ElMessage.success('登录成功')
    router.push('/tasks')
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  if (!regForm.username || !regForm.password) return ElMessage.warning('请输入账号和密码')
  loading.value = true
  try {
    await store.register(regForm)
    ElMessage.success('注册成功，已自动登录')
    router.push('/tasks')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 430px;
  border-radius: 12px;
  padding: 8px 12px;
}
.title {
  text-align: center;
  margin: 8px 0 20px;
  color: #4a4a8a;
  font-size: 20px;
}
.tip {
  margin-top: 14px;
  text-align: center;
  color: #999;
  font-size: 12px;
}
</style>
