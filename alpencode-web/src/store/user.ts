import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { AcUser } from '@/api/types';
import { logout as logoutApi } from '@/api/auth';

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

  /** 仅清除本地状态（用于 401 被踢出等场景，避免死循环） */
  function clearLocal() {
    token.value = '';
    user.value = null;
    localStorage.removeItem('ac_token');
  }

  /** 完整登出：先调后端再清本地 */
  async function logout() {
    try {
      if (token.value) {
        await logoutApi();
      }
    } catch {
      // 即使后端登出失败也清除本地状态
    }
    clearLocal();
  }

  const isLoggedIn = () => !!token.value;

  return { token, user, setToken, setUser, logout, clearLocal, isLoggedIn };
});
