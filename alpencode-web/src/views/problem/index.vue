<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRoute } from 'vue-router';
import { Splitpanes, Pane } from 'splitpanes';
import 'splitpanes/dist/splitpanes.css';
import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/preview.css';

import { getProblemDetail } from '@/api/problem';
import { runCode, submitCode, getSubmitResult } from '@/api/judge';
import MonacoEditor from '@/components/MonacoEditor.vue';
import type { Problem, RunCodeResult } from '@/api/types';

const route = useRoute();
const problemId = Number(route.params.id);

const problem = ref<Problem | null>(null);
const language = ref('java');
const code = ref('');
const activeTab = ref('description');
const resultTab = ref('result');
const running = ref(false);
const submitting = ref(false);
const runResults = ref<RunCodeResult[]>([]);
const submitResult = ref<{ status: string; msg: string } | null>(null);

const difficultyMap: Record<number, { text: string; color: string }> = {
  1: { text: '简单', color: 'green' },
  2: { text: '中等', color: 'orange' },
  3: { text: '困难', color: 'red' },
};

const defaultCode: Record<string, string> = {
  java: `import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // 在这里写你的代码\n    }\n}\n`,
  python: `# 在这里写你的代码\n`,
};

async function loadProblem() {
  const res = await getProblemDetail(problemId);
  problem.value = res.data || res;
  code.value = defaultCode[language.value] || '';
}

function onLanguageChange(lang: string) {
  language.value = lang;
  code.value = defaultCode[lang] || '';
}

async function handleRun() {
  running.value = true;
  runResults.value = [];
  resultTab.value = 'result';
  try {
    const res = await runCode({ problemId, language: language.value, code: code.value });
    runResults.value = res.data || res || [];
  } catch (e: any) {
    runResults.value = [{ passed: false, output: '', expectedOutput: '', timeCost: 0, memoryCost: 0, errorLog: e.message }];
  } finally {
    running.value = false;
  }
}

let pollTimer: ReturnType<typeof setInterval> | null = null;

async function handleSubmit() {
  submitting.value = true;
  submitResult.value = { status: 'PENDING', msg: '提交中...' };
  resultTab.value = 'result';
  try {
    const res = await submitCode({ problemId, language: language.value, code: code.value });
    const submitId = (res.data || res)?.submitId;
    if (submitId) {
      // 轮询判题结果
      pollTimer = setInterval(async () => {
        try {
          const r = await getSubmitResult(submitId);
          const data = r.data || r;
          if (data.result !== 0 && data.result !== 1) {
            // 判题完成
            clearInterval(pollTimer!);
            pollTimer = null;
            submitting.value = false;
            const resultMap: Record<number, string> = { 2: 'AC', 3: 'WA', 4: 'TLE', 5: 'MLE', 6: 'RE', 7: 'CE' };
            submitResult.value = {
              status: resultMap[data.result] || 'UNKNOWN',
              msg: `通过 ${data.passCount}/${data.totalCount} 个测试用例 | 耗时 ${data.timeCost}ms | 内存 ${data.memoryCost}MB`,
            };
          }
        } catch { /* ignore poll error */ }
      }, 2000);
    }
  } catch (e: any) {
    submitResult.value = { status: 'ERROR', msg: e.message };
    submitting.value = false;
  }
}

onMounted(loadProblem);
onBeforeUnmount(() => { if (pollTimer) clearInterval(pollTimer); });
</script>

<template>
  <div style="height: 100vh; display: flex; flex-direction: column">
    <!-- 顶部工具栏 -->
    <div style="height: 48px; background: #001529; display: flex; align-items: center; justify-content: space-between; padding: 0 16px">
      <div style="display: flex; align-items: center; gap: 12px">
        <router-link to="/" style="color: #fff; font-weight: bold; text-decoration: none">⛰️ AlpenCode</router-link>
        <span v-if="problem" style="color: #ffffffa0; font-size: 14px">{{ problem.title }}</span>
      </div>
      <div style="display: flex; align-items: center; gap: 8px">
        <a-select :value="language" style="width: 120px" size="small" @change="onLanguageChange">
          <a-select-option value="java">Java</a-select-option>
          <a-select-option value="python">Python</a-select-option>
        </a-select>
        <a-button size="small" :loading="running" @click="handleRun">▶ Run Code</a-button>
        <a-button type="primary" size="small" :loading="submitting" @click="handleSubmit">Submit</a-button>
      </div>
    </div>

    <!-- 主体分栏 -->
    <Splitpanes style="flex: 1">
      <!-- 左侧：题目描述 -->
      <Pane :size="40" :min-size="20">
        <div style="height: 100%; overflow-y: auto; padding: 16px; background: #fff">
          <a-tabs v-model:activeKey="activeTab" size="small">
            <a-tab-pane key="description" tab="题目描述">
              <template v-if="problem">
                <div style="margin-bottom: 12px">
                  <a-tag :color="difficultyMap[problem.difficulty]?.color">
                    {{ difficultyMap[problem.difficulty]?.text }}
                  </a-tag>
                  <span style="color: #999; font-size: 12px; margin-left: 8px">
                    时间限制: {{ problem.timeLimit }}ms | 内存限制: {{ problem.memoryLimit }}MB
                  </span>
                </div>
                <MdPreview :model-value="problem.description || ''" />
              </template>
              <a-spin v-else />
            </a-tab-pane>
            <a-tab-pane key="submissions" tab="提交记录">
              <p style="color: #999">提交记录功能开发中...</p>
            </a-tab-pane>
          </a-tabs>
        </div>
      </Pane>

      <!-- 右侧：编辑器 + 结果 -->
      <Pane :size="60" :min-size="30">
        <Splitpanes horizontal>
          <!-- 代码编辑器 -->
          <Pane :size="65" :min-size="30">
            <MonacoEditor v-model="code" :language="language" style="height: 100%" />
          </Pane>
          <!-- 运行结果 -->
          <Pane :size="35" :min-size="15">
            <div style="height: 100%; overflow-y: auto; padding: 12px; background: #1e1e1e; color: #d4d4d4; font-family: 'Consolas', monospace; font-size: 13px">
              <a-tabs v-model:activeKey="resultTab" size="small" type="card" class="dark-tabs">
                <a-tab-pane key="result" tab="运行结果">
                  <!-- Run Code 结果 -->
                  <template v-if="runResults.length">
                    <div v-for="(r, i) in runResults" :key="i" style="margin-bottom: 12px; padding: 8px; border-radius: 4px; background: #2d2d2d">
                      <a-tag :color="r.passed ? 'green' : 'red'">{{ r.passed ? 'PASS' : 'FAIL' }}</a-tag>
                      <div v-if="r.errorLog" style="color: #f44; margin-top: 4px; white-space: pre-wrap">{{ r.errorLog }}</div>
                      <div v-else style="margin-top: 4px">
                        <div>输出: <code>{{ r.output }}</code></div>
                        <div>期望: <code>{{ r.expectedOutput }}</code></div>
                      </div>
                    </div>
                  </template>
                  <!-- Submit 结果 -->
                  <template v-else-if="submitResult">
                    <div style="padding: 8px">
                      <a-tag :color="submitResult.status === 'AC' ? 'green' : submitResult.status === 'PENDING' ? 'blue' : 'red'" style="font-size: 16px; padding: 4px 12px">
                        {{ submitResult.status }}
                      </a-tag>
                      <div style="margin-top: 8px">{{ submitResult.msg }}</div>
                    </div>
                  </template>
                  <template v-else>
                    <span style="color: #666">点击 Run Code 或 Submit 查看结果</span>
                  </template>
                </a-tab-pane>
              </a-tabs>
            </div>
          </Pane>
        </Splitpanes>
      </Pane>
    </Splitpanes>
  </div>
</template>

<style scoped>
.dark-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}
.dark-tabs :deep(.ant-tabs-tab) {
  color: #999;
  background: #2d2d2d !important;
  border-color: #444 !important;
}
.dark-tabs :deep(.ant-tabs-tab-active) {
  color: #fff !important;
}
</style>
