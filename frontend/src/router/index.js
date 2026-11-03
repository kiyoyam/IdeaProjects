import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/tasks',
    children: [
      { path: 'tasks', component: () => import('../views/TaskList.vue'), meta: { title: '任务广场' } },
      { path: 'publish', component: () => import('../views/TaskPublish.vue'), meta: { title: '发布任务' } },
      { path: 'task/:id', component: () => import('../views/TaskDetail.vue'), meta: { title: '任务详情' } },
      { path: 'my/published', component: () => import('../views/MyPublished.vue'), meta: { title: '我的发布' } },
      { path: 'my/accepted', component: () => import('../views/MyAccepted.vue'), meta: { title: '我的接单' } },
      { path: 'wallet', component: () => import('../views/Wallet.vue'), meta: { title: '钱包结算' } },
      { path: 'admin', component: () => import('../views/Admin.vue'), meta: { title: '管理后台', requiresAdmin: true } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const store = useUserStore()
  if (to.path !== '/login' && !store.token) return '/login'
  if (to.meta.requiresAdmin && !store.isAdmin) return '/tasks'
  return true
})

export default router
