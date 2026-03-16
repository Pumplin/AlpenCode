<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts';
import { getLatestReport } from '@/api/profile';

const router = useRouter();

interface CategoryStat { name: string; count: number; }
interface StatsSnapshot {
  totalSubmissions: number;
  solvedCount: number;
  acceptRate: number;
  easyCount: number;
  mediumCount: number;
  hardCount: number;
  waCount: number;
  tleCount: number;
  reCount: number;
  ceCount: number;
  daysSinceJoin: number;
  categoryStats: CategoryStat[];
}
interface ReportContent {
  overview: string;
  personalityTag: string;
  abilityLevel: string;
  abilityScore: number;
  difficultyComment: string;
  difficultyAdvice: string;
  categoryComment: string;
  strongCategories: string[];
  weakCategories: string[];
  categoryAdvice: string;
  errorComment: string;
  errorAdvice: string;
  summary: string;
  highlights: string[];
  suggestions: string[];
  nextGoal: string;
}

const stats = ref<StatsSnapshot | null>(null);
const report = ref<ReportContent | null>(null);
const loading = ref(true);

// Chart refs
const ringChartRef = ref<HTMLElement | null>(null);
const radarChartRef = ref<HTMLElement | null>(null);
const pieChartRef = ref<HTMLElement | null>(null);
let ringChart: echarts.ECharts | null = null;
let radarChart: echarts.ECharts | null = null;
let pieChart: echarts.ECharts | null = null;

// Section visibility
const sectionRefs = ref<HTMLElement[]>([]);
const visibleSections = ref<Set<number>>(new Set());
let observer: IntersectionObserver | null = null;

function setSectionRef(el: any, index: number) {
  if (el) sectionRefs.value[index] = el;
}

// Computed
const totalErrors = computed(() => {
  if (!stats.value) return 0;
  return (stats.value.waCount || 0) + (stats.value.tleCount || 0) + (stats.value.reCount || 0) + (stats.value.ceCount || 0);
});

const abilityLevelColor = computed(() => {
  const level = report.value?.abilityLevel || '';
  if (level.includes('竞赛')) return '#f0883e';
  if (level.includes('高级')) return '#58a6ff';
  if (level.includes('中级')) return '#3fb950';
  if (level.includes('初级')) return '#d29922';
  return '#8b949e';
});

async function loadData() {
  loading.value = true;
  try {
    const res = await getLatestReport();
    const data = res.data;
    if (!data || data.status !== 1) {
      router.replace('/profile');
      return;
    }
    stats.value = JSON.parse(data.statsSnapshot);
    // 兼容旧格式（纯文本）和新格式（JSON）
    try {
      const raw = data.reportContent;
      // 去掉可能的 markdown 代码块
      const cleaned = raw.replace(/^```json\s*/i, '').replace(/\s*```$/, '').trim();
      report.value = JSON.parse(cleaned);
    } catch {
      router.replace('/profile');
      return;
    }
  } catch {
    router.replace('/profile');
  } finally {
    loading.value = false;
  }
}

function initRingChart() {
  if (!ringChartRef.value || !stats.value) return;
  ringChart?.dispose();
  ringChart = echarts.init(ringChartRef.value);
  const s = stats.value;
  ringChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: '#1c2128', borderColor: '#30363d', textStyle: { color: '#c9d1d9' }, formatter: '{b}: {c}题 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#8b949e', fontSize: 13 }, itemGap: 20 },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#0d1117', borderWidth: 3 },
      label: { show: true, position: 'outside', color: '#c9d1d9', fontSize: 13, formatter: '{b}\n{c}题' },
      emphasis: { label: { fontSize: 15, fontWeight: 'bold' } },
      data: [
        { value: s.easyCount, name: '简单', itemStyle: { color: '#3fb950' } },
        { value: s.mediumCount, name: '中等', itemStyle: { color: '#d29922' } },
        { value: s.hardCount, name: '困难', itemStyle: { color: '#f85149' } },
      ].filter(d => d.value > 0),
    }],
  });
}

function initRadarChart() {
  if (!radarChartRef.value || !stats.value) return;
  const cats = (stats.value.categoryStats || []).slice(0, 8); // 最多8个分类
  if (cats.length === 0) return;
  radarChart?.dispose();
  radarChart = echarts.init(radarChartRef.value);
  const maxVal = Math.max(...cats.map(c => c.count), 1);
  radarChart.setOption({
    tooltip: { backgroundColor: '#1c2128', borderColor: '#30363d', textStyle: { color: '#c9d1d9' } },
    radar: {
      indicator: cats.map(c => ({ name: c.name, max: Math.ceil(maxVal * 1.3) })),
      axisName: { color: '#8b949e', fontSize: 12 },
      splitArea: { areaStyle: { color: ['rgba(88,166,255,0.03)', 'rgba(88,166,255,0.06)'] } },
      splitLine: { lineStyle: { color: '#21262d' } },
      axisLine: { lineStyle: { color: '#30363d' } },
      shape: 'polygon',
    },
    series: [{
      type: 'radar',
      data: [{
        value: cats.map(c => c.count),
        name: '通过题数',
        areaStyle: { color: 'rgba(88,166,255,0.12)' },
        lineStyle: { color: '#58a6ff', width: 2 },
        itemStyle: { color: '#58a6ff', borderColor: '#58a6ff', borderWidth: 2 },
        symbol: 'circle',
        symbolSize: 6,
      }],
    }],
  });
}

function initPieChart() {
  if (!pieChartRef.value || !stats.value) return;
  const s = stats.value;
  const errorData = [
    { value: s.waCount, name: 'Wrong Answer', itemStyle: { color: '#f85149' } },
    { value: s.tleCount, name: 'Time Limit', itemStyle: { color: '#d29922' } },
    { value: s.reCount, name: 'Runtime Error', itemStyle: { color: '#da3633' } },
    { value: s.ceCount, name: 'Compile Error', itemStyle: { color: '#8b949e' } },
  ].filter(d => d.value > 0);
  if (errorData.length === 0) return;
  pieChart?.dispose();
  pieChart = echarts.init(pieChartRef.value);
  pieChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: '#1c2128', borderColor: '#30363d', textStyle: { color: '#c9d1d9' }, formatter: '{b}: {c}次 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#8b949e', fontSize: 12 }, itemGap: 16 },
    series: [{
      type: 'pie',
      radius: ['35%', '62%'],
      center: ['50%', '44%'],
      itemStyle: { borderRadius: 5, borderColor: '#0d1117', borderWidth: 3 },
      label: { color: '#c9d1d9', fontSize: 12, formatter: '{b}\n{d}%' },
      emphasis: { label: { fontSize: 14, fontWeight: 'bold' } },
      data: errorData,
    }],
  });
}

function setupObserver() {
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      const idx = Number(entry.target.getAttribute('data-section'));
      if (entry.isIntersecting) {
        visibleSections.value.add(idx);
        if (idx === 1) nextTick(initRingChart);
        if (idx === 2) nextTick(initRadarChart);
        if (idx === 3) nextTick(initPieChart);
      }
    });
  }, { threshold: 0.25 });
  sectionRefs.value.forEach(el => { if (el) observer!.observe(el); });
}

function handleResize() {
  ringChart?.resize();
  radarChart?.resize();
  pieChart?.resize();
}

onMounted(async () => {
  await loadData();
  if (stats.value && report.value) {
    await nextTick();
    setupObserver();
    window.addEventListener('resize', handleResize);
  }
});

onUnmounted(() => {
  observer?.disconnect();
  window.removeEventListener('resize', handleResize);
  ringChart?.dispose();
  radarChart?.dispose();
  pieChart?.dispose();
});
</script>

<template>
  <div v-if="loading" class="report-loading">
    <div class="loading-spinner"></div>
    <p>加载报告中...</p>
  </div>

  <div v-else-if="stats && report" class="report-scroll-container">
    <!-- 固定返回按钮 -->
    <button class="back-btn" @click="router.push('/profile')">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
      返回
    </button>

    <!-- ===== Section 0: 总览 ===== -->
    <section class="report-section" :class="{ visible: visibleSections.has(0) }"
      :ref="(el: any) => setSectionRef(el, 0)" data-section="0">
      <div class="section-inner wide">
        <div class="section-badge">📊 总览</div>
        <h1 class="overview-title">你的编程之旅</h1>
        <div class="personality-row">
          <span class="personality-tag">{{ report.personalityTag }}</span>
          <span class="ability-level-badge" :style="{ color: abilityLevelColor, borderColor: abilityLevelColor }">
            {{ report.abilityLevel }}
          </span>
        </div>

        <!-- 能力分数环 -->
        <div class="score-ring-wrap">
          <svg class="score-ring" viewBox="0 0 120 120">
            <circle cx="60" cy="60" r="50" fill="none" stroke="#21262d" stroke-width="8"/>
            <circle cx="60" cy="60" r="50" fill="none" :stroke="abilityLevelColor" stroke-width="8"
              stroke-linecap="round"
              :stroke-dasharray="`${(report.abilityScore / 100) * 314} 314`"
              stroke-dashoffset="78.5"
              style="transition: stroke-dasharray 1.5s ease"/>
            <text x="60" y="55" text-anchor="middle" fill="#e6edf3" font-size="26" font-weight="700">{{ report.abilityScore }}</text>
            <text x="60" y="74" text-anchor="middle" fill="#8b949e" font-size="11">综合评分</text>
          </svg>
        </div>

        <!-- 4个核心数字 -->
        <div class="overview-stats">
          <div class="overview-stat">
            <span class="overview-number">{{ stats.daysSinceJoin }}</span>
            <span class="overview-label">刷题天数</span>
          </div>
          <div class="overview-stat">
            <span class="overview-number">{{ stats.totalSubmissions }}</span>
            <span class="overview-label">总提交</span>
          </div>
          <div class="overview-stat">
            <span class="overview-number accent-green">{{ stats.solvedCount }}</span>
            <span class="overview-label">通过题目</span>
          </div>
          <div class="overview-stat">
            <span class="overview-number" :style="{ color: stats.acceptRate >= 60 ? '#3fb950' : stats.acceptRate >= 40 ? '#d29922' : '#f85149' }">
              {{ stats.acceptRate?.toFixed(1) }}%
            </span>
            <span class="overview-label">通过率</span>
          </div>
        </div>

        <!-- 亮点徽章 -->
        <div class="highlights-row" v-if="report.highlights?.length">
          <span v-for="(h, i) in report.highlights" :key="i" class="highlight-badge">🏅 {{ h }}</span>
        </div>

        <p class="overview-text">{{ report.overview }}</p>
      </div>
    </section>

    <!-- ===== Section 1: 难度分布 ===== -->
    <section class="report-section" :class="{ visible: visibleSections.has(1) }"
      :ref="(el: any) => setSectionRef(el, 1)" data-section="1">
      <div class="section-inner wide">
        <div class="section-badge">🎯 难度分布</div>
        <h2 class="section-title">难度攻克情况</h2>

        <div class="difficulty-layout">
          <!-- 左：图表 -->
          <div class="chart-container" ref="ringChartRef"></div>
          <!-- 右：进度条详情 -->
          <div class="difficulty-details">
            <div class="diff-item">
              <div class="diff-header">
                <span class="diff-dot" style="background:#3fb950"></span>
                <span class="diff-name">简单</span>
                <span class="diff-count">{{ stats.easyCount }} 题</span>
              </div>
              <div class="diff-bar-bg">
                <div class="diff-bar" :style="{ width: stats.solvedCount ? (stats.easyCount / stats.solvedCount * 100) + '%' : '0%', background: '#3fb950' }"></div>
              </div>
            </div>
            <div class="diff-item">
              <div class="diff-header">
                <span class="diff-dot" style="background:#d29922"></span>
                <span class="diff-name">中等</span>
                <span class="diff-count">{{ stats.mediumCount }} 题</span>
              </div>
              <div class="diff-bar-bg">
                <div class="diff-bar" :style="{ width: stats.solvedCount ? (stats.mediumCount / stats.solvedCount * 100) + '%' : '0%', background: '#d29922' }"></div>
              </div>
            </div>
            <div class="diff-item">
              <div class="diff-header">
                <span class="diff-dot" style="background:#f85149"></span>
                <span class="diff-name">困难</span>
                <span class="diff-count">{{ stats.hardCount }} 题</span>
              </div>
              <div class="diff-bar-bg">
                <div class="diff-bar" :style="{ width: stats.solvedCount ? (stats.hardCount / stats.solvedCount * 100) + '%' : '0%', background: '#f85149' }"></div>
              </div>
            </div>
            <div class="advice-box" v-if="report.difficultyAdvice">
              <span class="advice-icon">💡</span>
              <span>{{ report.difficultyAdvice }}</span>
            </div>
          </div>
        </div>

        <p class="section-comment">{{ report.difficultyComment }}</p>
      </div>
    </section>

    <!-- ===== Section 2: 分类能力 ===== -->
    <section class="report-section" :class="{ visible: visibleSections.has(2) }"
      :ref="(el: any) => setSectionRef(el, 2)" data-section="2">
      <div class="section-inner wide">
        <div class="section-badge">🧩 分类能力</div>
        <h2 class="section-title">能力雷达图</h2>

        <div class="category-layout">
          <div class="chart-container" ref="radarChartRef"></div>
          <div class="category-details">
            <div class="tag-group" v-if="report.strongCategories?.length">
              <div class="tag-group-label">💪 擅长</div>
              <div class="tag-list">
                <span v-for="c in report.strongCategories" :key="c" class="tag tag-strong">{{ c }}</span>
              </div>
            </div>
            <div class="tag-group" v-if="report.weakCategories?.length">
              <div class="tag-group-label">📌 待加强</div>
              <div class="tag-list">
                <span v-for="c in report.weakCategories" :key="c" class="tag tag-weak">{{ c }}</span>
              </div>
            </div>
            <!-- 分类排行 Top5 -->
            <div class="category-rank" v-if="stats.categoryStats?.length">
              <div class="rank-title">分类通过数 Top {{ Math.min(5, stats.categoryStats.length) }}</div>
              <div v-for="(cs, i) in [...stats.categoryStats].sort((a,b)=>b.count-a.count).slice(0,5)" :key="cs.name" class="rank-item">
                <span class="rank-no" :class="['rank-' + (i+1)]">{{ i + 1 }}</span>
                <span class="rank-name">{{ cs.name }}</span>
                <div class="rank-bar-bg">
                  <div class="rank-bar" :style="{ width: (cs.count / stats.categoryStats[0].count * 100) + '%' }"></div>
                </div>
                <span class="rank-count">{{ cs.count }}</span>
              </div>
            </div>
            <div class="advice-box" v-if="report.categoryAdvice">
              <span class="advice-icon">💡</span>
              <span>{{ report.categoryAdvice }}</span>
            </div>
          </div>
        </div>

        <p class="section-comment">{{ report.categoryComment }}</p>
      </div>
    </section>

    <!-- ===== Section 3: 错误分析 ===== -->
    <section class="report-section" :class="{ visible: visibleSections.has(3) }"
      :ref="(el: any) => setSectionRef(el, 3)" data-section="3">
      <div class="section-inner wide">
        <div class="section-badge">🔍 错误分析</div>
        <h2 class="section-title">常见错误类型</h2>

        <div class="error-layout">
          <div class="chart-container" ref="pieChartRef"></div>
          <div class="error-details">
            <div class="error-stat-grid">
              <div class="error-stat-item" v-if="stats.waCount">
                <div class="error-stat-num" style="color:#f85149">{{ stats.waCount }}</div>
                <div class="error-stat-label">Wrong Answer</div>
                <div class="error-stat-pct">{{ totalErrors ? ((stats.waCount/totalErrors)*100).toFixed(0) : 0 }}%</div>
              </div>
              <div class="error-stat-item" v-if="stats.tleCount">
                <div class="error-stat-num" style="color:#d29922">{{ stats.tleCount }}</div>
                <div class="error-stat-label">Time Limit</div>
                <div class="error-stat-pct">{{ totalErrors ? ((stats.tleCount/totalErrors)*100).toFixed(0) : 0 }}%</div>
              </div>
              <div class="error-stat-item" v-if="stats.reCount">
                <div class="error-stat-num" style="color:#da3633">{{ stats.reCount }}</div>
                <div class="error-stat-label">Runtime Error</div>
                <div class="error-stat-pct">{{ totalErrors ? ((stats.reCount/totalErrors)*100).toFixed(0) : 0 }}%</div>
              </div>
              <div class="error-stat-item" v-if="stats.ceCount">
                <div class="error-stat-num" style="color:#8b949e">{{ stats.ceCount }}</div>
                <div class="error-stat-label">Compile Error</div>
                <div class="error-stat-pct">{{ totalErrors ? ((stats.ceCount/totalErrors)*100).toFixed(0) : 0 }}%</div>
              </div>
            </div>
            <div class="advice-box" v-if="report.errorAdvice">
              <span class="advice-icon">💡</span>
              <span>{{ report.errorAdvice }}</span>
            </div>
          </div>
        </div>

        <p class="section-comment">{{ report.errorComment }}</p>
      </div>
    </section>

    <!-- ===== Section 4: 综合评价 ===== -->
    <section class="report-section" :class="{ visible: visibleSections.has(4) }"
      :ref="(el: any) => setSectionRef(el, 4)" data-section="4">
      <div class="section-inner">
        <div class="section-badge">✨ 综合评价</div>
        <h2 class="section-title">AI 为你总结</h2>

        <p class="summary-text">{{ report.summary }}</p>

        <!-- 学习建议 -->
        <div class="suggestions" v-if="report.suggestions?.length">
          <h3 class="suggestions-title">💡 学习建议</h3>
          <ul class="suggestions-list">
            <li v-for="(s, i) in report.suggestions" :key="i">
              <span class="suggestion-num">{{ i + 1 }}</span>
              <span>{{ s }}</span>
            </li>
          </ul>
        </div>

        <!-- 下一阶段目标 -->
        <div class="next-goal" v-if="report.nextGoal">
          <div class="next-goal-label">🎯 下一阶段目标</div>
          <div class="next-goal-text">{{ report.nextGoal }}</div>
        </div>

        <button class="back-profile-btn" @click="router.push('/profile')">返回个人中心</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ===== Loading ===== */
.report-loading {
  height: 100vh; display: flex; flex-direction: column; align-items: center;
  justify-content: center; background: #0d1117; color: #8b949e; font-size: 16px; gap: 16px;
}
.loading-spinner {
  width: 32px; height: 32px; border-radius: 50%;
  border: 3px solid #21262d; border-top-color: #58a6ff;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== Scroll Container ===== */
.report-scroll-container {
  height: 100vh; overflow-y: auto; scroll-snap-type: y mandatory; background: #0d1117;
}

/* ===== Back Button ===== */
.back-btn {
  position: fixed; top: 20px; left: 20px; z-index: 100;
  display: flex; align-items: center; gap: 6px;
  background: rgba(22, 27, 34, 0.9); backdrop-filter: blur(8px);
  border: 1px solid #30363d; border-radius: 8px; color: #8b949e;
  padding: 7px 14px; font-size: 13px; cursor: pointer; transition: all 0.2s;
}
.back-btn:hover { color: #58a6ff; border-color: #58a6ff; }

/* ===== Section ===== */
.report-section {
  height: 100vh; scroll-snap-align: start;
  display: flex; align-items: center; justify-content: center;
  padding: 60px 24px 40px;
  opacity: 0; transform: translateY(24px);
  transition: opacity 0.7s ease, transform 0.7s ease;
}
.report-section.visible { opacity: 1; transform: translateY(0); }

.section-inner { max-width: 640px; width: 100%; text-align: center; }
.section-inner.wide { max-width: 900px; }

.section-badge {
  display: inline-block; background: rgba(88,166,255,0.1); color: #58a6ff;
  padding: 5px 16px; border-radius: 20px; font-size: 13px; margin-bottom: 16px;
}
.section-title { color: #e6edf3; font-size: 26px; font-weight: 700; margin: 0 0 20px; }
.section-comment { color: #8b949e; font-size: 14px; line-height: 1.8; margin-top: 16px; text-align: left; }

/* ===== Overview ===== */
.overview-title { color: #e6edf3; font-size: 32px; font-weight: 700; margin: 0 0 12px; }
.personality-row { display: flex; align-items: center; justify-content: center; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.personality-tag {
  background: linear-gradient(135deg, #58a6ff, #3fb950);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
  font-size: 20px; font-weight: 700;
}
.ability-level-badge {
  border: 1px solid; border-radius: 20px; padding: 3px 14px; font-size: 13px; font-weight: 600;
}

/* 能力分数环 */
.score-ring-wrap { display: flex; justify-content: center; margin-bottom: 20px; }
.score-ring { width: 120px; height: 120px; }

.overview-stats { display: flex; justify-content: center; gap: 28px; margin-bottom: 20px; flex-wrap: wrap; }
.overview-stat { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.overview-number { font-size: 32px; font-weight: 700; color: #58a6ff; line-height: 1.1; }
.overview-number.accent-green { color: #3fb950; }
.overview-label { font-size: 12px; color: #8b949e; }

.highlights-row { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; margin-bottom: 16px; }
.highlight-badge {
  background: rgba(88,166,255,0.08); border: 1px solid rgba(88,166,255,0.2);
  color: #58a6ff; padding: 4px 12px; border-radius: 20px; font-size: 12px;
}
.overview-text { color: #8b949e; font-size: 14px; line-height: 1.8; max-width: 560px; margin: 0 auto; }

/* ===== Difficulty Layout ===== */
.difficulty-layout { display: flex; gap: 24px; align-items: flex-start; }
.difficulty-layout .chart-container { flex: 0 0 280px; height: 280px; }
.difficulty-details { flex: 1; display: flex; flex-direction: column; gap: 14px; padding-top: 8px; }

.diff-item { display: flex; flex-direction: column; gap: 6px; }
.diff-header { display: flex; align-items: center; gap: 8px; }
.diff-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.diff-name { color: #c9d1d9; font-size: 14px; flex: 1; }
.diff-count { color: #8b949e; font-size: 13px; }
.diff-bar-bg { height: 6px; background: #21262d; border-radius: 3px; overflow: hidden; }
.diff-bar { height: 100%; border-radius: 3px; transition: width 1.2s ease; }

/* ===== Category Layout ===== */
.category-layout { display: flex; gap: 24px; align-items: flex-start; }
.category-layout .chart-container { flex: 0 0 280px; height: 280px; }
.category-details { flex: 1; display: flex; flex-direction: column; gap: 14px; }

.tag-group { display: flex; flex-direction: column; gap: 6px; }
.tag-group-label { font-size: 13px; color: #8b949e; }
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; }
.tag { padding: 3px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.tag-strong { background: rgba(63,185,80,0.12); color: #3fb950; border: 1px solid rgba(63,185,80,0.25); }
.tag-weak { background: rgba(248,81,73,0.1); color: #f85149; border: 1px solid rgba(248,81,73,0.2); }

.category-rank { display: flex; flex-direction: column; gap: 8px; }
.rank-title { font-size: 12px; color: #484f58; margin-bottom: 2px; }
.rank-item { display: flex; align-items: center; gap: 8px; }
.rank-no { width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; background: #21262d; color: #8b949e; flex-shrink: 0; }
.rank-1 { background: #d29922; color: #0d1117; }
.rank-2 { background: #8b949e; color: #0d1117; }
.rank-3 { background: #c9722c; color: #0d1117; }
.rank-name { font-size: 13px; color: #c9d1d9; min-width: 60px; }
.rank-bar-bg { flex: 1; height: 5px; background: #21262d; border-radius: 3px; overflow: hidden; }
.rank-bar { height: 100%; background: linear-gradient(90deg, #58a6ff, #3fb950); border-radius: 3px; transition: width 1.2s ease; }
.rank-count { font-size: 12px; color: #8b949e; min-width: 24px; text-align: right; }

/* ===== Error Layout ===== */
.error-layout { display: flex; gap: 24px; align-items: flex-start; }
.error-layout .chart-container { flex: 0 0 280px; height: 280px; }
.error-details { flex: 1; display: flex; flex-direction: column; gap: 16px; }

.error-stat-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.error-stat-item {
  background: #161b22; border: 1px solid #21262d; border-radius: 10px;
  padding: 14px; text-align: center;
}
.error-stat-num { font-size: 28px; font-weight: 700; line-height: 1.1; }
.error-stat-label { font-size: 11px; color: #8b949e; margin-top: 4px; }
.error-stat-pct { font-size: 12px; color: #484f58; margin-top: 2px; }

/* ===== Advice Box ===== */
.advice-box {
  display: flex; align-items: flex-start; gap: 8px;
  background: rgba(88,166,255,0.06); border: 1px solid rgba(88,166,255,0.15);
  border-radius: 8px; padding: 10px 14px; font-size: 13px; color: #8b949e; line-height: 1.6;
}
.advice-icon { flex-shrink: 0; font-size: 14px; }

/* ===== Summary Section ===== */
.summary-text { color: #c9d1d9; font-size: 15px; line-height: 1.9; margin-bottom: 24px; text-align: left; }

.suggestions {
  text-align: left; background: rgba(88,166,255,0.04);
  border: 1px solid #21262d; border-radius: 12px; padding: 18px 20px; margin-bottom: 20px;
}
.suggestions-title { color: #c9d1d9; font-size: 15px; font-weight: 600; margin: 0 0 12px; }
.suggestions-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 10px; }
.suggestions-list li { display: flex; align-items: flex-start; gap: 10px; color: #8b949e; font-size: 14px; line-height: 1.6; }
.suggestion-num {
  flex-shrink: 0; width: 20px; height: 20px; border-radius: 50%;
  background: rgba(88,166,255,0.15); color: #58a6ff;
  display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700;
}

.next-goal {
  background: linear-gradient(135deg, rgba(88,166,255,0.08), rgba(63,185,80,0.08));
  border: 1px solid rgba(88,166,255,0.2); border-radius: 12px;
  padding: 16px 20px; margin-bottom: 24px; text-align: left;
}
.next-goal-label { font-size: 13px; color: #58a6ff; margin-bottom: 6px; font-weight: 600; }
.next-goal-text { font-size: 15px; color: #c9d1d9; line-height: 1.6; }

.back-profile-btn {
  background: #21262d; border: 1px solid #30363d; color: #c9d1d9;
  padding: 10px 28px; border-radius: 8px; font-size: 14px; cursor: pointer; transition: all 0.2s;
}
.back-profile-btn:hover { border-color: #58a6ff; color: #58a6ff; }

/* ===== Responsive ===== */
@media (max-width: 700px) {
  .difficulty-layout, .category-layout, .error-layout { flex-direction: column; }
  .difficulty-layout .chart-container,
  .category-layout .chart-container,
  .error-layout .chart-container { flex: none; width: 100%; height: 240px; }
  .overview-stats { gap: 16px; }
  .overview-number { font-size: 26px; }
  .overview-title { font-size: 24px; }
  .error-stat-grid { grid-template-columns: 1fr 1fr; }
}
</style>
