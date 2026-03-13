<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { register } from '@/api/auth';

const router = useRouter();
const loading = ref(false);
const form = ref({ username: '', password: '', confirmPassword: '', email: '' });

async function handleRegister() {
  if (!form.value.username || !form.value.password) { message.warning('请填写用户名和密码'); return; }
  if (form.value.password !== form.value.confirmPassword) { message.warning('两次密码不一致'); return; }
  loading.value = true;
  try {
    const data: { username: string; password: string; email?: string } = {
      username: form.value.username, password: form.value.password,
    };
    if (form.value.email) data.email = form.value.email;
    await register(data);
    message.success('注册成功，请登录');
    router.push('/login');
  } catch {} finally { loading.value = false; }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo">⛰️ <span class="logo-text">AlpenCode</span></div>
      <p class="auth-sub">创建你的账号</p>
      <div class="field">
        <label>用户名</label>
        <input v-model="form.username" placeholder="请输入用户名" />
      </div>
      <div class="field">
        <label>邮箱（可选）</label>
        <input v-model="form.email" placeholder="请输入邮箱" />
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="请输入密码" />
      </div>
      <div class="field">
        <label>确认密码</label>
        <input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" @keyup.enter="handleRegister" />
      </div>
      <button class="auth-btn" :disabled="loading" @click="handleRegister">
        {{ loading ? '注册中...' : '注 册' }}
      </button>
      <p class="auth-link">已有账号？<router-link to="/login">去登录</router-link></p>
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
.field { margin-bottom: 16px; }
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
