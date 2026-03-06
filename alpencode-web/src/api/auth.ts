import request from './request';
import type { LoginParams, LoginResult, R } from './types';

/** 用户登录 */
export function login(data: LoginParams) {
  return request.post<any, R<LoginResult>>('/oj/user/login', data);
}

/** 用户注册 */
export function register(data: { username: string; password: string; email: string }) {
  return request.post<any, R<void>>('/oj/user/register', data);
}

/** 获取当前用户信息 */
export function getUserInfo() {
  return request.get<any, R<LoginResult['user']>>('/oj/user/info');
}
