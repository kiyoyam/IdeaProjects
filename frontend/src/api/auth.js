import request from './request'

export const register = (data) => request.post('/auth/register', data)
export const login = (data) => request.post('/auth/login', data)
export const getInfo = () => request.get('/auth/info')
export const recharge = (data) => request.post('/auth/recharge', data)
