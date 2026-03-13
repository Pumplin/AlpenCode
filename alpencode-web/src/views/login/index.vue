<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { login } from '@/api/auth';
import { useUserStore } from '@/store/user';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const form = ref({ username: '', password: '' });

async function handleLogin() {
  if (!form.value.username || !form.value.password) { message.warning('请输入用户名和密码'); return; }
  loading.value = true;
  try {
    const res = await login(form.value);
    const data = res.data || res;
    userStore.setToken(data.token);
    userStore.setUser(data.user);
    message.success('登录成功');
    router.push('/');
  } catch {} finally { loading.value = false; }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo">⛰️ <span class="logo-text">AlpenCode</span></div>
      <p class="auth-sub">AI 辅助编程训练平台</p>
      <div class="field">
        <label>用户名</label>
        <input v-model="form.username" placeholder="请输入用户名" @keyup.enter="handleLogin" />
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="请输入密码" @keyup.enter="handleLogin" />
      </div>
      <button class="auth-btn" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '登 录' }}
      </button>
      <p class="auth-link">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: #0d1117;
  background-image: radial-gradient(ellipse at 50% 0%, #1f6feb15 0%, transparent 60%);
}
.auth-card {
  width: 380px; padding: 40px 36px; background: #161b22;
  border: 1px solid #30363d; border-radius: 12px;
}
.auth-logo { font-size: 28px; text-align: center; margin-bottom: 4px; }
.logo-text { font-weight: 700; color: #58a6ff; letter-spacing: 1px; }
.auth-sub { text-align: center; color: #484f58; font-size: 13px; margin-bottom: 28px; }
.field { margin-bottom: 18px; }
.field label { display: block; font-size: 13px; color: #8b949e; margin-bottom: 6px; }
.field input {
  width: 100%; padding: 10px 12px; background: #0d1117; border: 1px solid #30363d;
  border-radius: 6px; color: #c9d1d9; font-size: 14px; outline: none; transition: border 0.15s;
}
.field input:focus { border-color: #58a6ff; }
.field input::placeholder { color: #484f58; }
.auth-btn {
  width: 100%; padding: 10px; background: #1f6feb; color: #fff; border: none;
  border-radius: 6px; font-size: 15px; font-weight: 600; cursor: pointer;
  margin-top: 8px; transition: background 0.15s;
}
.auth-btn:hover { background: #388bfd; }
.auth-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.auth-link { text-align: center; margin-top: 20px; font-size: 13px; color: #8b949e; }
.auth-link a { color: #58a6ff; text-decoration: none; }
.auth-link a:hover { text-decoration: underline; }
</style>
