export const TASK_STATUS = {
  PENDING: { label: '待接单', type: 'warning' },
  ACCEPTED: { label: '已接单', type: 'primary' },
  IN_PROGRESS: { label: '配送中', type: 'info' },
  FINISHED: { label: '待确认', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'danger' }
}

export const CATEGORY = {
  PICKUP: '取件',
  DELIVERY: '送件',
  BUY: '代买',
  HELP: '代办',
  OTHER: '其他'
}

/** 分类对应的图标组件名（图标已全局注册） */
export const CATEGORY_ICON = {
  PICKUP: 'Box',
  DELIVERY: 'Van',
  BUY: 'ShoppingCart',
  HELP: 'Service',
  OTHER: 'More'
}

export const ROLE = {
  STUDENT: '学生',
  RUNNER: '跑腿员',
  ADMIN: '管理员'
}

export const WITHDRAW_STATUS = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' }
}

/** 任务广场排序方式 */
export const SORT_OPTIONS = [
  { label: '最新发布', value: 'latest' },
  { label: '报酬最高', value: 'reward' },
  { label: '即将截止', value: 'deadline' }
]

/** 进行中的接单状态 */
export const ACTIVE_STATUSES = ['ACCEPTED', 'IN_PROGRESS']

/** 任务进度节点 */
export const PROGRESS_STEPS = ['发布', '接单', '配送', '送达', '完成']

/** 状态对应的进度节点下标（用于步骤条） */
export const statusStep = (s) =>
  ({ PENDING: 0, ACCEPTED: 1, IN_PROGRESS: 2, FINISHED: 3, COMPLETED: 4 }[s] ?? 0)

export const statusLabel = (s) => TASK_STATUS[s]?.label || s
export const statusType = (s) => TASK_STATUS[s]?.type || 'info'
export const categoryLabel = (c) => CATEGORY[c] || c
export const categoryIcon = (c) => CATEGORY_ICON[c] || 'More'
export const roleLabel = (r) => ROLE[r] || r
export const withdrawLabel = (s) => WITHDRAW_STATUS[s]?.label || s
export const withdrawType = (s) => WITHDRAW_STATUS[s]?.type || 'info'

/** 距离截止还有多少毫秒，无截止时间返回 null */
export function remainMs(deadline) {
  if (!deadline) return null
  const t = new Date(String(deadline).replace(' ', 'T')).getTime()
  if (Number.isNaN(t)) return null
  return t - Date.now()
}

/** 是否加急：24 小时内截止且尚未过期 */
export function isUrgent(deadline) {
  const ms = remainMs(deadline)
  return ms !== null && ms > 0 && ms <= 24 * 3600 * 1000
}

/** 是否已过期 */
export function isOverdue(deadline) {
  const ms = remainMs(deadline)
  return ms !== null && ms <= 0
}

/** 倒计时文案，如「剩 3 小时 12 分」 */
export function countdownText(deadline) {
  const ms = remainMs(deadline)
  if (ms === null) return ''
  if (ms <= 0) return '已过期'
  const minutes = Math.floor(ms / 60000)
  if (minutes < 60) return `剩 ${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `剩 ${hours} 小时 ${minutes % 60} 分`
  return `剩 ${Math.floor(hours / 24)} 天`
}
