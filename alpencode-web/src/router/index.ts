import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '@/store/user';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Main',
      component: () => import('@/views/main/index.vue'),
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/register/index.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/profile/index.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/report',
      name: 'Report',
      component: () => import('@/views/report/index.vue'),
      meta: { requiresAuth: true },
    },
  ],
});

// 路由守卫：检查认证状态
router.beforeEach((to, from, next) => {
  const userStore = useUserStore();
  const isLoggedIn = userStore.isLoggedIn();

  // 需要认证的页面
  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login');
    return;
  }

  // 已登录用户访问登录/注册页，重定向到首页
  if ((to.path === '/login' || to.path === '/register') && isLoggedIn) {
    next('/');
    return;
  }

  next();
});

export default router;
