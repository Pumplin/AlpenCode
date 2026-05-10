<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { Splitpanes, Pane } from 'splitpanes';
import 'splitpanes/dist/splitpanes.css';
import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/preview.css';

import { getProblemSimpleList, getProblemDetail, getCategoryList } from '@/api/problem';
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
const listHasMore = ref(true);
const listQuery = ref({
  pageNum: 1, pageSize: 30,
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

async function loadProblems(append = false) {
  if (listLoading.value) return;
  listLoading.value = true;
  try {
    const res = await getProblemSimpleList(listQuery.value) as any;
    const rows = res?.rows || res?.data?.rows || [];
    const total = res?.total || res?.data?.total || 0;
    if (append) {
      problems.value = [...problems.value, ...rows];
    } else {
      problems.value = rows;
    }
    listHasMore.value = problems.value.length < total;
  } finally { listLoading.value = false; }
}

function resetAndLoadProblems() {
  listQuery.value.pageNum = 1;
  listHasMore.value = true;
  loadProblems(false);
}

function onProblemListScroll(e: Event) {
  const el = e.target as HTMLElement;
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 50 && listHasMore.value && !listLoading.value) {
    listQuery.value.pageNum++;
    loadProblems(true);
  }
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
  // 切题时清空所有状态
  viewingSubmit.value = null; savedCode.value = '';
  runResults.value = []; submitResult.value = null; aiAnalysis.value = '';
  bottomTab.value = 'result';
  submissions.value = [];
  try {
    const res = await getProblemDetail(id);
    problem.value = res.data || res;
    code.value = getTemplate(language.value);
    loadSubmissions(); // 异步加载当前题目的提交记录
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
const submitResult = ref<{
  status: string; msg: string; result: number;
  errorLog?: string;
  failedCases?: { caseIndex: number; input: string; expectedOutput: string; output: string }[];
} | null>(null);
const bottomPanelVisible = ref(true);
const bottomTab = ref('result');
const aiAnalysis = ref('');
const aiLoading = ref(false);
const editorFullscreen = ref(false);

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen();
  } else {
    document.exitFullscreen();
  }
}
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
    runResults.value = [{ passed: false, input: '', output: '', expectedOutput: '', timeCost: 0, memoryCost: 0, errorLog: e.message }];
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
          // 解析 judgeDetails（后端返回 JSON 字符串）
          let failedCases: any[] = [];
          if (data.judgeDetails) {
            try {
              const details = typeof data.judgeDetails === 'string' ? JSON.parse(data.judgeDetails) : data.judgeDetails;
              failedCases = (details || []).filter((d: any) => !d.passed);
            } catch {}
          }
          submitResult.value = {
            status: info.text,
            msg: `${data.passCount}/${data.totalCount} passed | ${data.timeCost}ms | ${data.memoryCost}MB`,
            result: data.result,
            errorLog: data.errorLog || '',
            failedCases,
          };
          startAiAnalysis(submitId);
          loadSubmissions(); // 判题完成后刷新提交记录
        }
      } catch {}
    }, 2000);
  } catch (e: any) { submitResult.value = { status: 'Error', msg: e.message, result: -1 }; submitting.value = false; }
}

// ===== 提交记录（按题目维度） =====
const submissions = ref<Submit[]>([]);
const submissionsLoading = ref(false);
const viewingSubmit = ref<Submit | null>(null); // 正在查看的历史提交
const savedCode = ref(''); // 查看历史时暂存当前编辑中的代码

async function loadSubmissions() {
  if (!selectedProblemId.value) { submissions.value = []; return; }
  submissionsLoading.value = true;
  try {
    const res = await getMySubmissions({ pageNum: 1, pageSize: 50, problemId: selectedProblemId.value }) as any;
    submissions.value = res?.rows || [];
  } finally { submissionsLoading.value = false; }
}

function viewSubmission(s: Submit) {
  if (!viewingSubmit.value) savedCode.value = code.value; // 首次查看时暂存
  viewingSubmit.value = s;
  code.value = s.code || '';
  // 展示该次提交的结果
  runResults.value = [];
  const info = resultMap[s.result] || { text: 'Unknown', color: '#8b949e' };
  let failedCases: any[] = [];
  if (s.judgeDetails) {
    try {
      const details = typeof s.judgeDetails === 'string' ? JSON.parse(s.judgeDetails as any) : s.judgeDetails;
      failedCases = (details || []).filter((d: any) => !d.passed);
    } catch {}
  }
  submitResult.value = {
    status: info.text,
    msg: `${s.passCount}/${s.totalCount} passed | ${s.timeCost}ms | ${s.memoryCost}MB`,
    result: s.result,
    errorLog: s.errorLog || '',
    failedCases,
  };
  // 加载该次提交保存的 AI 分析结果
  aiAnalysis.value = s.aiAnalysis || '';
  bottomTab.value = 'result';
}

function exitSubmissionView() {
  code.value = savedCode.value;
  viewingSubmit.value = null;
  submitResult.value = null;
  savedCode.value = '';
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

onMounted(() => {
  loadProblems(); loadCategories();
  document.addEventListener('fullscreenchange', () => {
    editorFullscreen.value = !!document.fullscreenElement;
  });
});
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
        <button class="btn btn-ghost btn-icon-only" @click="toggleFullscreen" :title="editorFullscreen ? '退出全屏' : '全屏'">
          <svg v-if="!editorFullscreen" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/></svg>
          <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3"/></svg>
        </button>
        <select class="lang-select" :value="language" @change="onLanguageChange(($event.target as HTMLSelectElement).value)">
          <option value="java">Java</option>
          <option value="python">Python</option>
        </select>
        <button class="btn btn-run" :disabled="!problem || running" @click="handleRun">
          <span v-if="running" class="btn-spinner"></span>
          <span v-else class="btn-icon">▶</span>
          {{ running ? '运行中...' : '运行' }}
        </button>
        <button class="btn btn-submit" :disabled="!problem || submitting" @click="handleSubmit">
          <span v-if="submitting" class="btn-spinner"></span>
          <span v-else class="btn-icon">⬆</span>
          {{ submitting ? '判题中...' : '提交' }}
        </button>
        <div class="header-divider"></div>
        <template v-if="userStore.isLoggedIn()">
          <span class="user-name">{{ userStore.user?.username || '用户' }}</span>
          <button class="btn btn-ghost" @click="router.push('/profile')">个人中心</button>
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
            <div class="sidebar-header">
              <span class="sidebar-title">题库</span>
            </div>

            <!-- 题目列表 -->
            <div class="sidebar-search">
              <input v-model="searchText" class="search-input" placeholder="搜索题目..." />
            </div>
            <div class="sidebar-filters">
              <select v-model="listQuery.difficulty" class="filter-select" @change="resetAndLoadProblems()">
                <option :value="undefined">全部难度</option>
                <option :value="1">简单</option>
                <option :value="2">中等</option>
                <option :value="3">困难</option>
              </select>
              <select v-model="listQuery.categoryId" class="filter-select" @change="resetAndLoadProblems()">
                <option :value="undefined">全部分类</option>
                <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
            <div class="problem-list" v-if="!listLoading || problems.length" @scroll="onProblemListScroll">
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
              <div v-if="listLoading" class="sidebar-loading">加载中...</div>
              <div v-if="!listHasMore && problems.length" class="sidebar-empty">没有更多了</div>
            </div>
            <div v-if="listLoading && !problems.length" class="sidebar-loading">加载中...</div>
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
                    <MdPreview :model-value="problem.description || ''" theme="light" />
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
                      <button :class="['rtab', bottomTab === 'history' && 'active']" @click="bottomTab = 'history'">
                        📋 历史记录
                      </button>
                      <button class="rtab close-btn" @click="bottomPanelVisible = false" title="关闭面板">✕</button>
                    </div>
                    <div class="result-body">
                      <!-- 运行结果 -->
                      <template v-if="bottomTab === 'result'">
                        <!-- 查看历史提交时的提示条 -->
                        <div v-if="viewingSubmit" class="viewing-banner">
                          <span>正在查看提交 #{{ viewingSubmit.id }} 的代码</span>
                          <button class="btn-back" @click="exitSubmissionView">恢复编辑</button>
                        </div>

                        <!-- Run Code 结果 -->
                        <template v-if="runResults.length">
                          <!-- 有错误日志（CE/RE）：直接展示报错 -->
                          <template v-if="runResults.some(r => r.errorLog)">
                            <div class="run-case">
                              <span class="case-badge" style="background: #f8514922; color: #f85149;">✗ 错误</span>
                              <div class="case-error">{{ runResults.find(r => r.errorLog)?.errorLog }}</div>
                            </div>
                          </template>
                          <!-- 全部通过 -->
                          <template v-else-if="runResults.every(r => r.passed)">
                            <div class="submit-result-box">
                              <span class="submit-status" style="color: #3fb950;">✓ 全部通过</span>
                              <span class="submit-msg">{{ runResults.length }} 个样例全部通过</span>
                            </div>
                          </template>
                          <!-- 有未通过的用例 -->
                          <template v-else>
                            <div v-for="(r, i) in runResults.filter(r => !r.passed)" :key="i" class="run-case">
                              <span class="case-badge" style="background: #f8514922; color: #f85149;">✗ 未通过</span>
                              <div class="case-detail">
                                <div><span class="label">输入:</span> <code>{{ r.input }}</code></div>
                                <div><span class="label">期望输出:</span> <code>{{ r.expectedOutput }}</code></div>
                                <div><span class="label">实际输出:</span> <code>{{ r.output }}</code></div>
                              </div>
                            </div>
                          </template>
                        </template>

                        <!-- Submit 结果 -->
                        <template v-else-if="submitResult">
                          <!-- 判题中 -->
                          <template v-if="submitResult.result === -1 || submitResult.result === 0 || submitResult.result === 1">
                            <div class="submit-result-box">
                              <span class="submit-status" style="color: #1f6feb;">{{ submitResult.status }}</span>
                              <span class="submit-msg">{{ submitResult.msg }}</span>
                            </div>
                          </template>
                          <!-- AC -->
                          <template v-else-if="submitResult.result === 2">
                            <div class="submit-result-box">
                              <span class="submit-status" style="color: #3fb950;">✓ Accepted</span>
                              <span class="submit-msg">{{ submitResult.msg }}</span>
                            </div>
                          </template>
                          <!-- CE / RE 有 errorLog -->
                          <template v-else-if="submitResult.errorLog">
                            <div class="run-case">
                              <span class="case-badge" style="background: #f8514922; color: #f85149;">{{ submitResult.status }}</span>
                              <div class="case-error">{{ submitResult.errorLog }}</div>
                            </div>
                          </template>
                          <!-- WA / TLE / MLE 展示失败用例详情 -->
                          <template v-else>
                            <div class="submit-result-box" style="margin-bottom: 8px;">
                              <span class="submit-status" :style="{ color: resultMap[submitResult.result]?.color || '#f85149' }">{{ submitResult.status }}</span>
                              <span class="submit-msg">{{ submitResult.msg }}</span>
                            </div>
                            <template v-if="submitResult.failedCases?.length">
                              <div v-for="(fc, i) in submitResult.failedCases" :key="i" class="run-case">
                                <span class="case-badge" style="background: #f8514922; color: #f85149;">用例 #{{ fc.caseIndex + 1 }} 未通过</span>
                                <div class="case-detail">
                                  <div><span class="label">输入:</span> <code>{{ fc.input }}</code></div>
                                  <div><span class="label">期望输出:</span> <code>{{ fc.expectedOutput }}</code></div>
                                  <div><span class="label">实际输出:</span> <code>{{ fc.output }}</code></div>
                                </div>
                              </div>
                            </template>
                          </template>
                        </template>

                        <template v-else>
                          <div class="result-empty">点击「运行」或「提交」查看结果</div>
                        </template>
                      </template>
                      <!-- AI 分析 -->
                      <template v-if="bottomTab === 'ai'">
                        <div v-if="aiAnalysis" class="ai-content">
                          <MdPreview :model-value="aiAnalysis" theme="light" />
                        </div>
                        <div v-else-if="aiLoading" class="result-empty">AI 正在分析中...</div>
                        <div v-else class="result-empty">提交代码后，AI 将自动分析</div>
                      </template>
                      <!-- 历史记录 -->
                      <template v-if="bottomTab === 'history'">
                        <template v-if="!selectedProblemId">
                          <div class="result-empty">请先选择一道题目</div>
                        </template>
                        <template v-else-if="submissionsLoading">
                          <div class="result-empty">加载中...</div>
                        </template>
                        <template v-else-if="!submissions.length">
                          <div class="result-empty">暂无提交记录</div>
                        </template>
                        <template v-else>
                          <div
                            v-for="s in submissions" :key="s.id"
                            :class="['history-item', viewingSubmit?.id === s.id && 'active']"
                            @click="viewSubmission(s)"
                          >
                            <div class="history-top">
                              <span class="history-result" :style="{ color: resultMap[s.result]?.color }">
                                {{ resultMap[s.result]?.text }}
                              </span>
                              <span v-if="s.aiAnalysis" class="history-ai-badge" title="有 AI 分析">🤖</span>
                              <span class="history-lang">{{ s.language === 'java' ? 'Java' : 'Python' }}</span>
                              <span class="history-time">{{ s.createdAt }}</span>
                            </div>
                            <div class="history-bottom">
                              <span>{{ s.passCount }}/{{ s.totalCount }} passed</span>
                              <span>{{ s.timeCost }}ms</span>
                              <span>{{ s.memoryCost }}MB</span>
                            </div>
                          </div>
                        </template>
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
.ide-root { height: 100vh; display: flex; flex-direction: column; background: #ffffff; color: #333333; overflow: hidden; }

/* ===== 顶栏 ===== */
.ide-header {
  height: 48px; min-height: 48px;
  background: #ffffff;
  border-bottom: 1px solid #e1e4e8;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 16px; gap: 12px; z-index: 10;
}
.ide-header-left, .ide-header-right { display: flex; align-items: center; gap: 8px; }
.ide-header-center { flex: 1; text-align: center; }
.current-problem-title { color: #57606a; font-size: 13px; }
.logo { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.logo-icon { font-size: 20px; }
.logo-text { font-size: 16px; font-weight: 700; color: #1f6feb; letter-spacing: 0.5px; }
.header-divider { width: 1px; height: 20px; background: #d0d7de; margin: 0 4px; }
.user-name { color: #57606a; font-size: 13px; }

/* ===== 按钮 ===== */
.btn {
  border: none; border-radius: 6px; cursor: pointer; font-size: 13px;
  padding: 6px 14px; display: flex; align-items: center; gap: 4px;
  transition: all 0.15s;
}
.btn-icon { font-size: 11px; }
.btn-spinner {
  width: 11px; height: 11px; border-radius: 50%;
  border: 2px solid currentColor; border-top-color: transparent;
  animation: spin 0.6s linear infinite; display: inline-block; flex-shrink: 0;
}
@keyframes spin { to { transform: rotate(360deg); } }
.btn-run { background: #f6f8fa; color: #3fb950; border: 1px solid #3fb95044; }
.btn-run:hover { background: #3fb95018; }
.btn-run:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-submit { background: #1f6feb; color: #fff; }
.btn-submit:hover { background: #388bfd; }
.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-ghost { background: transparent; color: #57606a; }
.btn-ghost:hover { color: #333333; }
.btn-primary-sm { background: #1f6feb; color: #fff; padding: 4px 12px; border-radius: 4px; }
.lang-select {
  background: #ffffff; color: #333333; border: 1px solid #d0d7de;
  border-radius: 6px; padding: 4px 8px; font-size: 13px; outline: none; cursor: pointer;
}

/* ===== 主体 ===== */
.ide-body { flex: 1; overflow: hidden; padding: 6px; background: #f6f8fa; }

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
.sidebar { height: 100%; background: #ffffff; display: flex; flex-direction: column; border-radius: 8px; overflow: hidden; border: 1px solid #e1e4e8; }

.sidebar-search { padding: 8px; }
.search-input {
  width: 100%; padding: 6px 10px; background: #ffffff; border: 1px solid #d0d7de;
  border-radius: 6px; color: #333333; font-size: 13px; outline: none;
}
.search-input:focus { border-color: #1f6feb; }
.search-input::placeholder { color: #8c959f; }

.sidebar-filters { display: flex; gap: 4px; padding: 0 8px 8px; }
.filter-select {
  flex: 1; padding: 4px 6px; background: #ffffff; border: 1px solid #d0d7de;
  border-radius: 4px; color: #333333; font-size: 12px; outline: none; cursor: pointer;
}

.problem-list { flex: 1; overflow-y: auto; }
.problem-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 12px;
  cursor: pointer; border-left: 3px solid transparent; transition: all 0.1s;
  font-size: 13px;
}
.problem-item:hover { background: #f6f8fa; }
.problem-item.selected { background: #ddf4ff; border-left-color: #1f6feb; }
.problem-id { color: #8c959f; font-size: 12px; min-width: 28px; }
.problem-title-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #333333; }
.problem-diff { font-size: 11px; font-weight: 600; white-space: nowrap; }

.sidebar-loading, .sidebar-empty { padding: 24px; text-align: center; color: #8c959f; font-size: 13px; }

/* ===== 题目描述面板 ===== */
.desc-panel { height: 100%; overflow-y: auto; padding: 20px; background: #ffffff; border-radius: 8px; border: 1px solid #e1e4e8; }
.desc-header { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.desc-title { font-size: 18px; font-weight: 700; color: #1f2328; margin: 0; }
.diff-badge { padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; }
.desc-meta { display: flex; gap: 16px; font-size: 12px; color: #57606a; margin-bottom: 16px; }
.desc-content { font-size: 14px; line-height: 1.7; }

.panel-placeholder {
  height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #d0d7de;
}
.placeholder-icon { font-size: 64px; margin-bottom: 16px; }
.placeholder-text { font-size: 15px; color: #8c959f; }

/* ===== 编辑器面板 ===== */
.editor-panel { height: 100%; background: #ffffff; border-radius: 8px; overflow: hidden; border: 1px solid #e1e4e8; }

.btn-icon-only { padding: 5px 7px; }
.btn-icon-only svg { display: block; }

/* ===== 结果面板 ===== */
.result-panel { height: 100%; display: flex; flex-direction: column; background: #ffffff; border-radius: 8px; overflow: hidden; border: 1px solid #e1e4e8; }
.result-tabs { display: flex; align-items: center; border-bottom: 1px solid #e1e4e8; padding: 0 8px; }
.rtab {
  padding: 8px 14px; font-size: 13px; background: transparent; border: none;
  color: #57606a; cursor: pointer; border-bottom: 2px solid transparent; transition: all 0.15s;
}
.rtab.active { color: #1f6feb; border-bottom-color: #1f6feb; }
.rtab:hover { color: #333333; }
.close-btn { margin-left: auto; font-size: 14px; padding: 8px 10px; }
.loading-dot { color: #1f6feb; animation: blink 1s infinite; margin-left: 4px; font-size: 10px; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }

.result-body { flex: 1; overflow-y: auto; padding: 12px 16px; font-size: 13px; }
.result-empty { color: #8c959f; padding: 20px; text-align: center; }

.run-case { padding: 8px 12px; background: #f6f8fa; border-radius: 6px; margin-bottom: 8px; border: 1px solid #e1e4e8; }
.case-badge { padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; }
.case-error { color: #f85149; margin-top: 6px; white-space: pre-wrap; font-family: 'Consolas', monospace; font-size: 12px; }
.case-detail { margin-top: 6px; font-family: 'Consolas', monospace; }
.case-detail .label { color: #57606a; }
.case-detail code { color: #1f2328; }

.submit-result-box { display: flex; flex-direction: column; gap: 8px; padding: 12px; }
.submit-status { font-size: 20px; font-weight: 700; }
.submit-msg { color: #57606a; font-size: 13px; }

.ai-content { font-size: 14px; line-height: 1.6; }
.ai-content :deep(.md-editor-preview-wrapper) { padding: 0; }
.ai-content :deep(pre) { background: #f6f8fa !important; border-radius: 6px; border: 1px solid #e1e4e8; }

/* ===== 左侧标题栏 ===== */
.sidebar-header { padding: 10px 12px; border-bottom: 1px solid #e1e4e8; }
.sidebar-title { font-size: 13px; font-weight: 600; color: #333333; }

/* ===== 历史记录 ===== */
.history-item {
  padding: 10px 12px; border-bottom: 1px solid #e1e4e8; cursor: pointer;
  transition: background 0.1s; border-left: 3px solid transparent;
}
.history-item:hover { background: #f6f8fa; }
.history-item.active { background: #ddf4ff; border-left-color: #1f6feb; }
.history-top { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.history-result { font-weight: 600; font-size: 13px; }
.history-ai-badge { font-size: 12px; cursor: default; }
.history-lang { color: #57606a; font-size: 12px; }
.history-time { color: #8c959f; font-size: 12px; margin-left: auto; }
.history-bottom { display: flex; gap: 12px; color: #8c959f; font-size: 12px; }

/* ===== 查看历史提示条 ===== */
.viewing-banner {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px; margin-bottom: 8px;
  background: #ddf4ff; border: 1px solid #54aeff; border-radius: 6px;
  font-size: 13px; color: #0969da;
}
.btn-back {
  background: #ffffff; color: #333333; border: 1px solid #d0d7de;
  border-radius: 4px; padding: 3px 10px; font-size: 12px; cursor: pointer;
  transition: all 0.15s;
}
.btn-back:hover { background: #f6f8fa; }
</style>
