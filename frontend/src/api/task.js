import request from './request'

export const publishTask = (data) => request.post('/tasks', data)
export const queryTasks = (params) => request.get('/tasks', { params })
export const taskDetail = (id) => request.get(`/tasks/${id}`)
export const acceptTask = (id) => request.post(`/tasks/${id}/accept`)
export const startTask = (id) => request.post(`/tasks/${id}/start`)
export const finishTask = (id) => request.post(`/tasks/${id}/finish`)
export const confirmTask = (id) => request.post(`/tasks/${id}/confirm`)
export const cancelTask = (id, reason) => request.post(`/tasks/${id}/cancel`, { reason })
export const myPublished = (params) => request.get('/tasks/mine/published', { params })
export const myAccepted = (params) => request.get('/tasks/mine/accepted', { params })
export const myOrders = (params) => request.get('/orders', { params })
