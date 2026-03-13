<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { Splitpanes, Pane } from 'splitpanes';
import 'splitpanes/dist/splitpanes.css';
import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/preview.css';

import { getProblemList, getProblemDetail, getCategoryList } from '@/api/problem';
import { runCode, submitCode, getSubmitResult } from '@/api/judge';
import { getMySubmissions } from '@/api/submit';
import { createAiAnalysisStream } from '@/api/ai';
import MonacoEditor from '@/components/MonacoEditor.vue';
import { useUserStore } from '@/store/user';
import type { Problem, ProblemCategory, RunCodeResult, Submit } from '@/api/types';

const router = useRouter();
const userStore = useUserStore();

// ===== 题目列表 =====
const problems = ref<Problem[]>([]);
const categories = ref<ProblemCategory[]>([]);
const listLoading = ref(false);
const listQuery = ref({
  pageNum: 1, pageSize: 100,
  categoryId: undefined as number | undefined,
  difficulty: undefined as number | undefined,
  title: '',
});
const selectedProblemId = ref<number | null>(null);

const difficultyMap: Record<number, { text: string; color: string }> = {
  1: { text: '简单', color: '#3fb950' },
  2: { text: '中等', color: '#d29922' },
  3: { text: '困难', color: '#f85149' },
};
const resultMap: Record<number, { text: string; color: string }> = {
  0: { text: 'Pending', color: '#8b949e' },
  1: { text: 'Judging', color: '#1f6feb' },
  2: { text: 'Accepted', color: '#3fb950' },
  3: { text: 'Wrong Answer', color: '#f85149' },
  4: { text: 'TLE', color: '#d29922' },
  5: { text: 'MLE', color: '#d29922' },
  6: { text: 'Runtime Error', color: '#f85149' },
  7: { text: 'Compile Error', color: '#f85149' },
};

async function loadProblems() {
  listLoading.value = true;
  try {
    const res = await getProblemList(listQuery.value) as any;
    problems.value = res?.rows || [];
  } finally { listLoading.value = false; }
}
async function loadCategories() {
  try { const res = await getCategoryList(); categories.value = res.data || res || []; } catch {}
}

function selectProblem(id: number) { selectedProblemId.value = id; }

// ===== 题目详情 =====
const problem = ref<Problem | null>(null);
const detailLoading = ref(false);

watch(selectedProblemId, async (id) => {
  if (!id) { problem.value = null; return; }
  detailLoading.value = true;
  try {
    const res = await getProblemDetail(id);
    problem.value = res.data || res;
    code.value = getTemplate(language.value);
    runResults.value = []; submitResult.value = null; aiAnalysis.value = '';
  } finally { detailLoading.value = false; }
});

// ===== 编辑器 =====
const language = ref('java');
const code = ref('');
const fallbackCode: Record<string, string> = {
  java: `class Solution {\n    // 在这里写你的代码\n}\n`,
  python: `class Solution:\n    # 在这里写你的代码\n    pass\n`,
};
function getTemplate(lang: string): string {
  return problem.value?.codeTemplates?.[lang] || fallbackCode[lang] || '';
}
function onLanguageChange(lang: string) { language.value = lang; code.value = getTemplate(lang); }

// ===== 运行 & 提交 =====
const running = ref(false);
const submitting = ref(false);
const runResults = ref<RunCodeResult[]>([]);
const submitResult = ref<{ status: string; msg: string; result: number } | null>(null);
const bottomPanelVisible = ref(true);
const bottomTab = ref('result');
const aiAnalysis = ref('');
const aiLoading = ref(false);
let closeAiStream: (() => void) | null = null;
let pollTimer: ReturnType<typeof setInterval> | null = null;

function startAiAnalysis(submitId: number) {
  aiAnalysis.value = ''; aiLoading.value = true; bottomTab.value = 'ai';
  closeAiStream = createAiAnalysisStream(submitId,
    (token) => { aiAnalysis.value += token; },
    () => { aiLoading.value = false; },
    () => { aiLoading.value = false; if (!aiAnalysis.value) aiAnalysis.value = 'AI 分析暂时不可用'; },
  );
}

async function handleRun() {
  if (!problem.value) return;
  running.value = true; runResults.value = []; submitResult.value = null;
  bottomPanelVisible.value = true; bottomTab.value = 'result';
  try {
    const res = await runCode({ problemId: problem.value.id, language: language.value, code: code.value });
    runResults.value = res.data || res || [];
  } catch (e: any) {
    runResults.value = [{ passed: false, output: '', expectedOutput: '', timeCost: 0, memoryCost: 0, errorLog: e.message }];
  } finally { running.value = false; }
}

async function handleSubmit() {
  if (!problem.value) return;
  submitting.value = true;
  submitResult.value = { status: 'Judging...', msg: '正在判题...', result: -1 };
  runResults.value = []; bottomPanelVisible.value = true; bottomTab.value = 'result'; aiAnalysis.value = '';
  try {
    const res = await submitCode({ problemId: problem.value.id, language: language.value, code: code.value });
    const submitId = (res.data || res)?.submitId;
    if (!submitId) return;
    pollTimer = setInterval(async () => {
      try {
        const r = await getSubmitResult(submitId);
        const data = r.data || r;
        if (data.result !== 0 && data.result !== 1) {
          clearInterval(pollTimer!); pollTimer = null; submitting.value = false;
          const info = resultMap[data.result] || { text: 'Unknown', color: '#8b949e' };
          submitResult.value = { status: info.text, msg: `${data.passCount}/${data.totalCount} passed | ${data.timeCost}ms | ${data.memoryCost}MB`, result: data.result };
          startAiAnalysis(submitId);
        }
      } catch {}
    }, 2000);
  } catch (e: any) { submitResult.value = { status: 'Error', msg: e.message, result: -1 }; submitting.value = false; }
}

// ===== 提交记录 =====
const submissions = ref<Submit[]>([]);
const submissionsLoading = ref(false);
const leftTab = ref<'problems' | 'submissions'>('problems');
watch(leftTab, (tab) => { if (tab === 'submissions') loadSubmissions(); });
async function loadSubmissions() {
  submissionsLoading.value = true;
  try {
    const res = await getMySubmissions({ pageNum: 1, pageSize: 50 });
    const data = res.data as any;
    submissions.value = data?.rows || [];
  } finally { submissionsLoading.value = false; }
}

// ===== 搜索 =====
const searchText = ref('');
const filteredProblems = computed(() => {
  if (!searchText.value) return problems.value;
  const s = searchText.value.toLowerCase();
  return problems.value.filter(p => p.title.toLowerCase().includes(s) || String(p.id).includes(s));
});

function getAcRate(p: Problem) {
  if (!p.submitCount) return '-';
  return ((p.acCount / p.submitCount) * 100).toFixed(0) + '%';
}

onMounted(() => { loadProblems(); loadCategories(); });
onBeforeUnmount(() => { if (pollTimer) clearInterval(pollTimer); if (closeAiStream) closeAiStream(); });
</script>

<template>
  <div class="ide-root">
    <!-- ===== 顶栏 ===== -->
    <header class="ide-header">
      <div class="ide-header-left">
        <div class="logo" @click="router.push('/')">
          <span class="logo-icon">⛰️</span>
          <span class="logo-text">AlpenCode</span>
        </div>
      </div>
      <div class="ide-header-center">
        <span v-if="problem" class="current-problem-title">{{ problem.title }}</span>
      </div>
      <div class="ide-header-right">
        <select class="lang-select" :value="language" @change="onLanguageChange(($event.target as HTMLSelectElement).value)">
          <option value="java">Java</option>
          <option value="python">Python</option>
        </select>
        <button class="btn btn-run" :disabled="!problem || running" @click="handleRun">
          <span class="btn-icon">▶</span> 运行
        </button>
        <button class="btn btn-submit" :disabled="!problem || submitting" @click="handleSubmit">
          <span class="btn-icon">⬆</span> 提交
        </button>
        <div class="header-divider"></div>
        <template v-if="userStore.isLoggedIn()">
          <span class="user-name">{{ userStore.user?.username || '用户' }}</span>
          <button class="btn btn-ghost" @click="userStore.logout(); router.push('/login')">退出</button>
        </template>
        <template v-else>
          <button class="btn btn-ghost" @click="router.push('/login')">登录</button>
          <button class="btn btn-primary-sm" @click="router.push('/register')">注册</button>
        </template>
      </div>
    </header>

    <!-- ===== 主体 ===== -->
    <div class="ide-body">
      <Splitpanes class="dark-splitpanes">
        <!-- 左侧：题目列表 / 提交记录 -->
        <Pane :size="18" :min-size="12" :max-size="30">
          <div class="sidebar">
            <div class="sidebar-tabs">
              <button :class="['stab', leftTab === 'problems' && 'active']" @click="leftTab = 'problems'">题库</button>
              <button :class="['stab', leftTab === 'submissions' && 'active']" @click="leftTab = 'submissions'">记录</button>
            </div>

            <!-- 题目列表 -->
            <template v-if="leftTab === 'problems'">
              <div class="sidebar-search">
                <input v-model="searchText" class="search-input" placeholder="搜索题目..." />
              </div>
              <div class="sidebar-filters">
                <select v-model="listQuery.difficulty" class="filter-select" @change="loadProblems()">
                  <option :value="undefined">全部难度</option>
                  <option :value="1">简单</option>
                  <option :value="2">中等</option>
                  <option :value="3">困难</option>
                </select>
                <select v-model="listQuery.categoryId" class="filter-select" @change="loadProblems()">
                  <option :value="undefined">全部分类</option>
                  <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
                </select>
              </div>
              <div class="problem-list" v-if="!listLoading">
                <div
                  v-for="p in filteredProblems" :key="p.id"
                  :class="['problem-item', selectedProblemId === p.id && 'selected']"
                  @click="selectProblem(p.id)"
                >
                  <span class="problem-id">{{ p.id }}</span>
                  <span class="problem-title-text">{{ p.title }}</span>
                  <span class="problem-diff" :style="{ color: difficultyMap[p.difficulty]?.color }">
                    {{ difficultyMap[p.difficulty]?.text }}
                  </span>
                </div>
              </div>
              <div v-else class="sidebar-loading">加载中...</div>
            </template>

            <!-- 提交记录 -->
            <template v-if="leftTab === 'submissions'">
              <div class="submission-list" v-if="!submissionsLoading">
                <div v-for="s in submissions" :key="s.id" class="submission-item">
                  <div class="sub-top">
                    <span class="sub-problem">{{ s.problemTitle || `#${s.problemId}` }}</span>
                    <span class="sub-result" :style="{ color: resultMap[s.result]?.color }">
                      {{ resultMap[s.result]?.text }}
                    </span>
                  </div>
                  <div class="sub-bottom">
                    <span>{{ s.language }}</span>
                    <span>{{ s.timeCost }}ms</span>
                    <span>{{ s.passCount }}/{{ s.totalCount }}</span>
                  </div>
                </div>
                <div v-if="!submissions.length" class="sidebar-empty">暂无提交记录</div>
              </div>
              <div v-else class="sidebar-loading">加载中...</div>
            </template>
          </div>
        </Pane>

        <!-- 中间 + 右侧 -->
        <Pane :size="82">
          <Splitpanes>
            <!-- 中间：题目描述 -->
            <Pane :size="35" :min-size="20">
              <div class="desc-panel">
                <template v-if="problem">
                  <div class="desc-header">
                    <h2 class="desc-title">{{ problem.title }}</h2>
                    <span class="diff-badge" :style="{ background: difficultyMap[problem.difficulty]?.color + '22', color: difficultyMap[problem.difficulty]?.color }">
                      {{ difficultyMap[problem.difficulty]?.text }}
                    </span>
                  </div>
                  <div class="desc-meta">
                    <span>⏱ {{ problem.timeLimit }}ms</span>
                    <span>💾 {{ problem.memoryLimit }}MB</span>
                    <span>📊 通过率 {{ getAcRate(problem) }}</span>
                  </div>
                  <div class="desc-content">
                    <MdPreview :model-value="problem.description || ''" theme="dark" />
                  </div>
                </template>
                <template v-else-if="detailLoading">
                  <div class="panel-placeholder">加载中...</div>
                </template>
                <template v-else>
                  <div class="panel-placeholder">
                    <div class="placeholder-icon">⛰️</div>
                    <div class="placeholder-text">从左侧选择一道题目开始</div>
                  </div>
                </template>
              </div>
            </Pane>

            <!-- 右侧：编辑器 + 结果 -->
            <Pane :size="65" :min-size="30">
              <Splitpanes horizontal>
                <!-- 编辑器 -->
                <Pane :size="bottomPanelVisible ? 65 : 100" :min-size="30">
                  <div class="editor-panel">
                    <MonacoEditor v-model="code" :language="language" style="height: 100%" />
                  </div>
                </Pane>
                <!-- 底部结果面板 -->
                <Pane v-if="bottomPanelVisible" :size="35" :min-size="15">
                  <div class="result-panel">
                    <div class="result-tabs">
                      <button :class="['rtab', bottomTab === 'result' && 'active']" @click="bottomTab = 'result'">运行结果</button>
                      <button :class="['rtab', bottomTab === 'ai' && 'active']" @click="bottomTab = 'ai'">
                        {{ submitResult?.result === 2 ? '✨ 优化建议' : '🔍 AI 诊断' }}
                        <span v-if="aiLoading" class="loading-dot">●</span>
                      </button>
                      <button class="rtab close-btn" @click="bottomPanelVisible = false" title="关闭面板">✕</button>
                    </div>
                    <div class="result-body">
                      <!-- 运行结果 -->
                      <template v-if="bottomTab === 'result'">
                        <template v-if="runResults.length">
                          <div v-for="(r, i) in runResults" :key="i" class="run-case">
                            <span class="case-badge" :style="{ background: r.passed ? '#3fb95022' : '#f8514922', color: r.passed ? '#3fb950' : '#f85149' }">
                              {{ r.passed ? '✓ 通过' : '✗ 未通过' }}
                            </span>
                            <div v-if="r.errorLog" class="case-error">{{ r.errorLog }}</div>
                            <div v-else class="case-detail">
                              <div><span class="label">输出:</span> <code>{{ r.output }}</code></div>
                              <div><span class="label">期望:</span> <code>{{ r.expectedOutput }}</code></div>
                            </div>
                          </div>
                        </template>
                        <template v-else-if="submitResult">
                          <div class="submit-result-box">
                            <span class="submit-status" :style="{ color: submitResult.result === 2 ? '#3fb950' : submitResult.result === -1 ? '#1f6feb' : '#f85149' }">
                              {{ submitResult.status }}
                            </span>
                            <span class="submit-msg">{{ submitResult.msg }}</span>
                          </div>
                        </template>
                        <template v-else>
                          <div class="result-empty">点击「运行」或「提交」查看结果</div>
                        </template>
                      </template>
                      <!-- AI 分析 -->
                      <template v-if="bottomTab === 'ai'">
                        <div v-if="aiAnalysis" class="ai-content">
                          <MdPreview :model-value="aiAnalysis" theme="dark" />
                        </div>
                        <div v-else-if="aiLoading" class="result-empty">AI 正在分析中...</div>
                        <div v-else class="result-empty">提交代码后，AI 将自动分析</div>
                      </template>
                    </div>
                  </div>
                </Pane>
              </Splitpanes>
            </Pane>
          </Splitpanes>
        </Pane>
      </Splitpanes>
    </div>
  </div>
</template>

<style scoped>
/* ===== 根布局 ===== */
.ide-root { height: 100vh; display: flex; flex-direction: column; background: #0d1117; color: #c9d1d9; overflow: hidden; }

/* ===== 顶栏 ===== */
.ide-header {
  height: 48px; min-height: 48px;
  background: linear-gradient(180deg, #161b22 0%, #0d1117 100%);
  border-bottom: 1px solid #21262d;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 16px; gap: 12px; z-index: 10;
}
.ide-header-left, .ide-header-right { display: flex; align-items: center; gap: 8px; }
.ide-header-center { flex: 1; text-align: center; }
.current-problem-title { color: #8b949e; font-size: 13px; }
.logo { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.logo-icon { font-size: 20px; }
.logo-text { font-size: 16px; font-weight: 700; color: #58a6ff; letter-spacing: 0.5px; }
.header-divider { width: 1px; height: 20px; background: #30363d; margin: 0 4px; }
.user-name { color: #8b949e; font-size: 13px; }

/* ===== 按钮 ===== */
.btn {
  border: none; border-radius: 6px; cursor: pointer; font-size: 13px;
  padding: 6px 14px; display: flex; align-items: center; gap: 4px;
  transition: all 0.15s;
}
.btn-icon { font-size: 11px; }
.btn-run { background: #21262d; color: #3fb950; border: 1px solid #3fb95044; }
.btn-run:hover { background: #3fb95018; }
.btn-run:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-submit { background: #1f6feb; color: #fff; }
.btn-submit:hover { background: #388bfd; }
.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-ghost { background: transparent; color: #8b949e; }
.btn-ghost:hover { color: #c9d1d9; }
.btn-primary-sm { background: #1f6feb; color: #fff; padding: 4px 12px; border-radius: 4px; }
.lang-select {
  background: #21262d; color: #c9d1d9; border: 1px solid #30363d;
  border-radius: 6px; padding: 4px 8px; font-size: 13px; outline: none; cursor: pointer;
}

/* ===== 主体 ===== */
.ide-body { flex: 1; overflow: hidden; padding: 6px; background: #010409; }

/* ===== Splitpanes 间距 + 圆角面板 ===== */
:deep(.splitpanes__splitter) {
  background: transparent !important;
  border: none !important;
}
:deep(.splitpanes--vertical > .splitpanes__splitter) {
  width: 6px !important;
  min-width: 6px !important;
}
:deep(.splitpanes--horizontal > .splitpanes__splitter) {
  height: 6px !important;
  min-height: 6px !important;
}
:deep(.splitpanes__splitter::before),
:deep(.splitpanes__splitter::after) {
  display: none !important;
}

/* ===== 左侧边栏 ===== */
.sidebar { height: 100%; background: #0d1117; display: flex; flex-direction: column; border-radius: 8px; overflow: hidden; }
.sidebar-tabs { display: flex; border-bottom: 1px solid #21262d; }
.stab {
  flex: 1; padding: 10px 0; text-align: center; font-size: 13px; font-weight: 500;
  background: transparent; border: none; color: #8b949e; cursor: pointer;
  border-bottom: 2px solid transparent; transition: all 0.15s;
}
.stab.active { color: #58a6ff; border-bottom-color: #58a6ff; }
.stab:hover { color: #c9d1d9; }

.sidebar-search { padding: 8px; }
.search-input {
  width: 100%; padding: 6px 10px; background: #161b22; border: 1px solid #30363d;
  border-radius: 6px; color: #c9d1d9; font-size: 13px; outline: none;
}
.search-input:focus { border-color: #58a6ff; }
.search-input::placeholder { color: #484f58; }

.sidebar-filters { display: flex; gap: 4px; padding: 0 8px 8px; }
.filter-select {
  flex: 1; padding: 4px 6px; background: #161b22; border: 1px solid #30363d;
  border-radius: 4px; color: #c9d1d9; font-size: 12px; outline: none; cursor: pointer;
}

.problem-list { flex: 1; overflow-y: auto; }
.problem-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 12px;
  cursor: pointer; border-left: 3px solid transparent; transition: all 0.1s;
  font-size: 13px;
}
.problem-item:hover { background: #161b22; }
.problem-item.selected { background: #1f6feb15; border-left-color: #58a6ff; }
.problem-id { color: #484f58; font-size: 12px; min-width: 28px; }
.problem-title-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #c9d1d9; }
.problem-diff { font-size: 11px; font-weight: 600; white-space: nowrap; }

.submission-list { flex: 1; overflow-y: auto; }
.submission-item { padding: 10px 12px; border-bottom: 1px solid #21262d; font-size: 12px; }
.sub-top { display: flex; justify-content: space-between; margin-bottom: 4px; }
.sub-problem { color: #c9d1d9; font-weight: 500; }
.sub-result { font-weight: 600; }
.sub-bottom { display: flex; gap: 12px; color: #484f58; }
.sidebar-loading, .sidebar-empty { padding: 24px; text-align: center; color: #484f58; font-size: 13px; }

/* ===== 题目描述面板 ===== */
.desc-panel { height: 100%; overflow-y: auto; padding: 20px; background: #0d1117; border-radius: 8px; }
.desc-header { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.desc-title { font-size: 18px; font-weight: 700; color: #e6edf3; margin: 0; }
.diff-badge { padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; }
.desc-meta { display: flex; gap: 16px; font-size: 12px; color: #8b949e; margin-bottom: 16px; }
.desc-content { font-size: 14px; line-height: 1.7; }

.panel-placeholder {
  height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #30363d;
}
.placeholder-icon { font-size: 64px; margin-bottom: 16px; }
.placeholder-text { font-size: 15px; color: #484f58; }

/* ===== 编辑器面板 ===== */
.editor-panel { height: 100%; background: #0d1117; border-radius: 8px; overflow: hidden; }

/* ===== 结果面板 ===== */
.result-panel { height: 100%; display: flex; flex-direction: column; background: #0d1117; border-radius: 8px; overflow: hidden; }
.result-tabs { display: flex; align-items: center; border-bottom: 1px solid #21262d; padding: 0 8px; }
.rtab {
  padding: 8px 14px; font-size: 13px; background: transparent; border: none;
  color: #8b949e; cursor: pointer; border-bottom: 2px solid transparent; transition: all 0.15s;
}
.rtab.active { color: #58a6ff; border-bottom-color: #58a6ff; }
.rtab:hover { color: #c9d1d9; }
.close-btn { margin-left: auto; font-size: 14px; padding: 8px 10px; }
.loading-dot { color: #1f6feb; animation: blink 1s infinite; margin-left: 4px; font-size: 10px; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }

.result-body { flex: 1; overflow-y: auto; padding: 12px 16px; font-size: 13px; }
.result-empty { color: #484f58; padding: 20px; text-align: center; }

.run-case { padding: 8px 12px; background: #161b22; border-radius: 6px; margin-bottom: 8px; }
.case-badge { padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }
.case-error { color: #f85149; margin-top: 6px; white-space: pre-wrap; font-family: 'Consolas', monospace; font-size: 12px; }
.case-detail { margin-top: 6px; font-family: 'Consolas', monospace; }
.case-detail .label { color: #8b949e; }
.case-detail code { color: #e6edf3; }

.submit-result-box { display: flex; flex-direction: column; gap: 8px; padding: 12px; }
.submit-status { font-size: 20px; font-weight: 700; }
.submit-msg { color: #8b949e; font-size: 13px; }

.ai-content { font-size: 14px; line-height: 1.6; }
.ai-content :deep(.md-editor-preview-wrapper) { padding: 0; }
.ai-content :deep(pre) { background: #161b22 !important; border-radius: 6px; }
</style>
