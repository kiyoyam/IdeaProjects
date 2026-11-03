import request from './request'

export const evaluate = (data) => request.post('/evaluations', data)
export const taskEvaluations = (taskId) => request.get(`/evaluations/task/${taskId}`)
