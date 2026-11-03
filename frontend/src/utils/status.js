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

export const statusLabel = (s) => TASK_STATUS[s]?.label || s
export const statusType = (s) => TASK_STATUS[s]?.type || 'info'
export const categoryLabel = (c) => CATEGORY[c] || c
export const roleLabel = (r) => ROLE[r] || r
export const withdrawLabel = (s) => WITHDRAW_STATUS[s]?.label || s
export const withdrawType = (s) => WITHDRAW_STATUS[s]?.type || 'info'
