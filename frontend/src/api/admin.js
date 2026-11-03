import request from './request'

export const adminUsers = (params) => request.get('/admin/users', { params })
export const adminTasks = (params) => request.get('/admin/tasks', { params })
export const adminSettlements = (params) => request.get('/admin/settlements', { params })
export const adminWithdrawals = (params) => request.get('/admin/withdrawals', { params })
export const approveWithdrawal = (id) => request.post(`/admin/withdrawals/${id}/approve`)
export const rejectWithdrawal = (id, remark) => request.post(`/admin/withdrawals/${id}/reject`, { remark })
