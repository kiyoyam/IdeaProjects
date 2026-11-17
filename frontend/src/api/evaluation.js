import request from './request'

export const evaluate = (data) => request.post('/evaluations', data)
export const taskEvaluations = (taskId) => request.get(`/evaluations/task/${taskId}`)
/** 待评价订单（我参与且已完成、尚未评价） */
export const pendingEvaluations = (params) => request.get('/evaluations/pending', { params })
/** 我收到的评价 */
export const receivedEvaluations = (params) => request.get('/evaluations/received', { params })
/** 我发出的评价 */
export const sentEvaluations = (params) => request.get('/evaluations/sent', { params })
/** 我的评价汇总 */
export const myEvaluationSummary = () => request.get('/evaluations/summary/me')
