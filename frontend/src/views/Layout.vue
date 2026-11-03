<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="brand">
        <el-icon :size="22" color="#5b6bf5"><Van /></el-icon>
        <span>校园跑腿</span>
      </div>
      <el-menu mode="horizontal" :default-active="activeMenu" router class="nav" :ellipsis="false">
        <el-menu-item index="/tasks">任务广场</el-menu-item>
        <el-menu-item index="/publish">发布任务</el-menu-item>
        <el-menu-item index="/my/published">我的发布</el-menu-item>
        <el-menu-item index="/my/accepted">我的接单</el-menu-item>
        <el-menu-item index="/wallet">钱包结算</el-menu-item>
        <el-menu-item v-if="store.isAdmin" index="/admin">管理后台</el-menu-item>
      </el-menu>
      <div class="right">
        <el-dropdown @command="onCommand">
          <span class="user">
            {{ store.user?.nickname || store.user?.username || '未登录' }}
            <el-tag size="small" style="margin-left: 6px">{{ roleLabel(store.user?.role) }}</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="wallet">钱包：¥{{ store.user?.balance ?? '--' }}</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { roleLabel } from '../utils/status'

const store = useUserStore()
const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => (route.path.startsWith('/task/') ? '/tasks' : route.path))

onMounted(() => {
  if (!store.user) store.fetchInfo().catch(() => {})
})

function onCommand(cmd) {
  if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  } else if (cmd === 'wallet') {
    router.push('/wallet')
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 24px;
  gap: 24px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 700;
  color: #5b6bf5;
  white-space: nowrap;
}
.nav {
  flex: 1;
  border-bottom: none;
}
.right {
  white-space: nowrap;
}
.user {
  cursor: pointer;
  color: #333;
  font-size: 14px;
}
.main {
  background: #f5f7fa;
  padding: 20px;
}
</style>
