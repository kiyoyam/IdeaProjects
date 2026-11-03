import request from './request'

export const mySettlements = (params) => request.get('/settlements', { params })
export const walletInfo = () => request.get('/settlements/wallet')
export const withdraw = (data) => request.post('/settlements/withdrawals', data)
export const myWithdrawals = (params) => request.get('/settlements/withdrawals', { params })
