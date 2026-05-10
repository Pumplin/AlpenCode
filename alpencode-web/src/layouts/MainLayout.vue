<script setup lang="ts">
import { useRouter } from 'vue-router';
import { useUserStore } from '@/store/user';

const router = useRouter();
const userStore = useUserStore();
</script>

<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header style="display: flex; align-items: center; justify-content: space-between; padding: 0 24px; background: #ffffff; border-bottom: 1px solid #e1e4e8">
      <div style="display: flex; align-items: center; gap: 24px">
        <div style="color: #1f6feb; font-size: 18px; font-weight: bold; cursor: pointer" @click="router.push('/')">
          ⛰️ AlpenCode
        </div>
        <a-menu theme="light" mode="horizontal" :style="{ lineHeight: '64px', border: 'none', background: 'transparent' }">
          <a-menu-item key="home" @click="router.push('/')">题库</a-menu-item>
          <a-menu-item key="submissions" @click="router.push('/submissions')">提交记录</a-menu-item>
        </a-menu>
      </div>
      <div>
        <template v-if="userStore.isLoggedIn()">
          <a-dropdown>
            <span style="color: #333333; cursor: pointer">
              {{ userStore.user?.username || '用户' }}
            </span>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="userStore.logout(); router.push('/login')">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
        <template v-else>
          <a-button type="link" style="color: #1f6feb" @click="router.push('/login')">登录</a-button>
          <a-button type="primary" size="small" @click="router.push('/register')">注册</a-button>
        </template>
      </div>
    </a-layout-header>
    <a-layout-content style="padding: 24px; background: #ffffff">
      <RouterView />
    </a-layout-content>
  </a-layout>
</template>
