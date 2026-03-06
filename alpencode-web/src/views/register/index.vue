<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { register } from '@/api/auth';

const router = useRouter();
const loading = ref(false);
const form = ref({ username: '', password: '', confirmPassword: '', email: '' });

async function handleRegister() {
  if (!form.value.username || !form.value.password || !form.value.email) {
    message.warning('请填写完整信息');
    return;
  }
  if (form.value.password !== form.value.confirmPassword) {
    message.warning('两次密码不一致');
    return;
  }
  loading.value = true;
  try {
    await register({ username: form.value.username, password: form.value.password, email: form.value.email });
    message.success('注册成功，请登录');
    router.push('/login');
  } catch { /* error handled by interceptor */ }
  finally { loading.value = false; }
}
</script>

<template>
  <div style="display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f0f2f5">
    <a-card style="width: 400px" title="⛰️ AlpenCode 注册">
      <a-form layout="vertical">
        <a-form-item label="用户名">
          <a-input v-model:value="form.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="邮箱">
          <a-input v-model:value="form.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="密码">
          <a-input-password v-model:value="form.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item label="确认密码">
          <a-input-password v-model:value="form.confirmPassword" placeholder="请再次输入密码" @press-enter="handleRegister" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" block :loading="loading" @click="handleRegister">注册</a-button>
        </a-form-item>
        <div style="text-align: center">
          已有账号？<router-link to="/login">去登录</router-link>
        </div>
      </a-form>
    </a-card>
  </div>
</template>
