import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        { path: '', name: 'Home', component: () => import('@/views/home/index.vue') },
        { path: 'submissions', name: 'Submissions', component: () => import('@/views/submission/index.vue') },
      ],
    },
    {
      path: '/problem/:id',
      name: 'Problem',
      component: () => import('@/views/problem/index.vue'),
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/register/index.vue'),
    },
  ],
});

export default router;
