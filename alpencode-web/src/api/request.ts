import axios from 'axios';
import { message } from 'ant-design-vue';
import { useUserStore } from '@/store/user';
import router from '@/router';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
});

// 请求拦截：注入 ac_user token
request.interceptors.request.use((config) => {
  const userStore = useUserStore();
  if (userStore.token) {
    config.headers['Authorization'] = `Bearer ${userStore.token}`;
  }
  return config;
});

// 处理认证失败的统一逻辑
function handleAuthError() {
  const userStore = useUserStore();
  userStore.clearLocal();
  router.push('/login');
  message.error('登录已过期，请重新登录');
}

// 响应拦截：统一处理错误
request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== undefined && res.code !== 200) {
      // 401 未登录或 token 过期，清除本地状态并跳转登录页
      if (res.code === 401) {
        handleAuthError();
        return Promise.reject(new Error('未授权'));
      }
      message.error(res.msg || '请求失败');
      return Promise.reject(new Error(res.msg || '请求失败'));
    }
    return res;
  },
  (error) => {
    // 处理 HTTP 状态码 401（token 验证失败）
    if (error.response?.status === 401) {
      handleAuthError();
      return Promise.reject(error);
    }
    
    // 处理其他错误
    const errorMsg = error.response?.data?.msg || error.message || '网络错误';
    message.error(errorMsg);
    return Promise.reject(error);
  },
);

export default request;
