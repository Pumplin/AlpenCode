<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { useUserStore } from '@/store/user';
import { getUserInfo } from '@/api/auth';
import { updateProfile, updatePassword, getStats, generateReport, getLatestReport } from '@/api/profile';
import type { AcUserStatsVo, AcAiReportVo } from '@/api/types';

const router = useRouter();
const userStore = useUserStore();

// --- Stats ---
const statsLoading = ref(false);
const stats = ref<AcUserStatsVo | null>(null);

// --- Report ---
const report = ref<AcAiReportVo | null>(null);
const reportLoading = ref(false);
let pollTimer: ReturnType<typeof setInterval> | null = null;

// --- Modals ---
const infoModalVisible = ref(false);
const passwordModalVisible = ref(false);

// --- Info form ---
const infoForm = reactive({ username: '', email: '' });
const infoSubmitting = ref(false);

// --- Password form ---
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });
const passwordSubmitting = ref(false);
const passwordError = ref('');

const acceptRate = computed(() => {
  if (!stats.value) return '0%';
  return stats.value.acceptRate != null ? stats.value.acceptRate.toFixed(1) + '%' : '0%';
});

// --- Load stats ---
async function loadStats() {
  statsLoading.value = true;
  try {
    const res = await getStats();
    stats.value = res.data;
  } catch { /* ignore */ }
  finally { statsLoading.value = false; }
}

// --- Load report ---
async function loadReport() {
  try {
    const res = await getLatestReport();
    report.value = res.data;
    // Start polling if generating
    if (res.data && res.data.status === 0) {
      startPolling();
    } else {
      stopPolling();
    }
  } catch { /* ignore */ }
}

function startPolling() {
  stopPolling();
  pollTimer = setInterval(async () => {
    try {
      const res = await getLatestReport();
      report.value = res.data;
      if (!res.data || res.data.status !== 0) {
        stopPolling();
      }
    } catch { stopPolling(); }
  }, 3000);
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

// --- Generate report ---
async function handleGenerateReport() {
  reportLoading.value = true;
  try {
    await generateReport();
    message.success('报告生成已触发');
    await loadReport();
  } catch {
    message.error('触发报告生成失败');
  } finally {
    reportLoading.value = false;
  }
}

// --- View report ---
function handleViewReport() {
  router.push('/report');
}

// --- Info modal ---
function openInfoModal() {
  infoForm.username = userStore.user?.username || '';
  infoForm.email = userStore.user?.email || '';
  infoModalVisible.value = true;
}

async function handleInfoSubmit() {
  infoSubmitting.value = true;
  try {
    await updateProfile({ username: infoForm.username, email: infoForm.email });
    message.success('信息修改成功');
    infoModalVisible.value = false;
    // Refresh user info in store
    const res = await getUserInfo();
    if (res.data) userStore.setUser(res.data);
  } catch (e: any) {
    message.error(e?.response?.data?.msg || '修改失败');
  } finally {
    infoSubmitting.value = false;
  }
}

// --- Password modal ---
function openPasswordModal() {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
  passwordError.value = '';
  passwordModalVisible.value = true;
}

async function handlePasswordSubmit() {
  passwordError.value = '';
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = '两次密码不一致';
    return;
  }
  if (passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 20) {
    passwordError.value = '密码长度必须在6-20之间';
    return;
  }
  passwordSubmitting.value = true;
  try {
    await updatePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword });
    message.success('密码修改成功');
    passwordModalVisible.value = false;
  } catch (e: any) {
    message.error(e?.response?.data?.msg || '密码修改失败');
  } finally {
    passwordSubmitting.value = false;
  }
}

onMounted(() => {
  loadStats();
  loadReport();
});

onUnmounted(() => {
  stopPolling();
});
</script>

<template>
  <div class="profile-page">
    <div class="profile-container">
      <!-- 用户信息卡片 -->
      <a-card class="profile-card" :bordered="false">
        <div class="card-header">
          <div class="avatar-circle">{{ userStore.user?.username?.charAt(0)?.toUpperCase() || 'U' }}</div>
          <div class="user-info">
            <h2 class="username">{{ userStore.user?.username || '用户' }}</h2>
            <p class="email">{{ userStore.user?.email || '未设置邮箱' }}</p>
            <p class="join-info" v-if="stats">已加入 {{ stats.daysSinceJoin }} 天</p>
          </div>
        </div>
        <div class="card-actions">
          <a-button @click="openInfoModal">修改信息</a-button>
          <a-button @click="openPasswordModal">修改密码</a-button>
        </div>
      </a-card>

      <!-- 刷题概览 -->
      <a-card class="profile-card" :bordered="false" :loading="statsLoading">
        <h3 class="section-title">📊 刷题概览</h3>
        <div class="stats-grid" v-if="stats">
          <div class="stat-item">
            <div class="stat-number">{{ stats.totalSubmissions }}</div>
            <div class="stat-label">总提交</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ stats.solvedCount }}</div>
            <div class="stat-label">通过题目</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ acceptRate }}</div>
            <div class="stat-label">通过率</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ stats.daysSinceJoin }}</div>
            <div class="stat-label">注册天数</div>
          </div>
        </div>
        <div v-else class="empty-stats">暂无刷题数据</div>
      </a-card>

      <!-- AI 报告入口 -->
      <a-card class="profile-card" :bordered="false">
        <h3 class="section-title">🤖 AI 能力报告</h3>
        <div class="report-section">
          <!-- 无报告 -->
          <template v-if="!report">
            <p class="report-desc">基于你的刷题数据，AI 将为你生成个性化能力分析报告</p>
            <a-button type="primary" :loading="reportLoading" @click="handleGenerateReport">
              生成 AI 报告
            </a-button>
          </template>

          <!-- 生成中 -->
          <template v-else-if="report.status === 0">
            <div class="report-generating">
              <a-spin />
              <span style="margin-left: 12px">报告生成中...</span>
            </div>
          </template>

          <!-- 已完成 -->
          <template v-else-if="report.status === 1">
            <p class="report-desc">你的 AI 能力报告已生成</p>
            <a-space>
              <a-button type="primary" @click="handleViewReport">查看报告</a-button>
              <a-button :loading="reportLoading" @click="handleGenerateReport">重新生成</a-button>
            </a-space>
          </template>

          <!-- 生成失败 -->
          <template v-else-if="report.status === 2">
            <p class="report-desc" style="color: #f5222d">报告生成失败</p>
            <a-button type="primary" :loading="reportLoading" @click="handleGenerateReport">
              重新生成
            </a-button>
          </template>
        </div>
      </a-card>
    </div>

    <!-- 修改信息 Modal -->
    <a-modal
      v-model:open="infoModalVisible"
      title="修改信息"
      :confirm-loading="infoSubmitting"
      @ok="handleInfoSubmit"
      @cancel="infoModalVisible = false"
    >
      <a-form layout="vertical">
        <a-form-item label="用户名">
          <a-input v-model:value="infoForm.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="邮箱">
          <a-input v-model:value="infoForm.email" placeholder="请输入邮箱" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 修改密码 Modal -->
    <a-modal
      v-model:open="passwordModalVisible"
      title="修改密码"
      :confirm-loading="passwordSubmitting"
      @ok="handlePasswordSubmit"
      @cancel="passwordModalVisible = false"
    >
      <a-form layout="vertical">
        <a-form-item label="旧密码">
          <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入旧密码" />
        </a-form-item>
        <a-form-item label="新密码">
          <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码（6-20位）" />
        </a-form-item>
        <a-form-item label="确认新密码" :validate-status="passwordError ? 'error' : ''" :help="passwordError">
          <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.profile-page {
  min-height: calc(100vh - 64px);
  background: #0d1117;
  padding: 32px 16px;
}

.profile-container {
  max-width: 720px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
  background: #161b22;
  border-radius: 12px;
}

.profile-card :deep(.ant-card-body) {
  padding: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.avatar-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #58a6ff, #3fb950);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: bold;
  color: #fff;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
}

.username {
  color: #c9d1d9;
  font-size: 22px;
  margin: 0 0 4px 0;
  font-weight: 600;
}

.email {
  color: #8b949e;
  margin: 0 0 2px 0;
  font-size: 14px;
}

.join-info {
  color: #8b949e;
  margin: 0;
  font-size: 13px;
}

.card-actions {
  display: flex;
  gap: 12px;
}

.card-actions :deep(.ant-btn) {
  background: #21262d;
  border-color: #30363d;
  color: #c9d1d9;
}

.card-actions :deep(.ant-btn:hover) {
  border-color: #58a6ff;
  color: #58a6ff;
}

.section-title {
  color: #c9d1d9;
  font-size: 18px;
  margin: 0 0 20px 0;
  font-weight: 600;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-item {
  text-align: center;
  padding: 16px 8px;
  background: #21262d;
  border-radius: 8px;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #58a6ff;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #8b949e;
  margin-top: 6px;
}

.empty-stats {
  text-align: center;
  color: #8b949e;
  padding: 24px 0;
}

.report-section {
  text-align: center;
  padding: 12px 0;
}

.report-desc {
  color: #8b949e;
  margin-bottom: 16px;
  font-size: 14px;
}

.report-generating {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c9d1d9;
  padding: 16px 0;
  font-size: 15px;
}

.report-generating :deep(.ant-spin-dot-item) {
  background-color: #58a6ff;
}

/* Ant Design Vue overrides for dark theme */
.profile-card :deep(.ant-card-loading-content) {
  padding: 0;
}

.profile-card :deep(.ant-card-loading-block) {
  background: linear-gradient(90deg, #21262d 25%, #30363d 37%, #21262d 63%);
}

@media (max-width: 600px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
