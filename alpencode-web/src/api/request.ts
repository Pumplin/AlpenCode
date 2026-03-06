import axios from 'axios';
import { message } from 'ant-design-vue';
import { useUserStore } from '@/store/user';

const request = axios.create({
  baseURL: '/api',
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

// 响应拦截：统一处理错误
request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== undefined && res.code !== 200) {
      message.error(res.msg || '请求失败');
      // 401 未登录，跳转登录页
      if (res.code === 401) {
        const userStore = useUserStore();
        userStore.logout();
        window.location.hash = '#/login';
      }
      return Promise.reject(new Error(res.msg || '请求失败'));
    }
    return res;
  },
  (error) => {
    message.error(error.message || '网络错误');
    return Promise.reject(error);
  },
);

export default request;
