import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { AcUser } from '@/api/types';

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('ac_token') || '');
  const user = ref<AcUser | null>(null);

  function setToken(t: string) {
    token.value = t;
    localStorage.setItem('ac_token', t);
  }

  function setUser(u: AcUser) {
    user.value = u;
  }

  function logout() {
    token.value = '';
    user.value = null;
    localStorage.removeItem('ac_token');
  }

  const isLoggedIn = () => !!token.value;

  return { token, user, setToken, setUser, logout, isLoggedIn };
});
