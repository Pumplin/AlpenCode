<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getMySubmissions } from '@/api/submit';
import type { Submit } from '@/api/types';

const loading = ref(false);
const submissions = ref<Submit[]>([]);
const total = ref(0);
const query = ref({ pageNum: 1, pageSize: 20 });

const resultMap: Record<number, { text: string; color: string }> = {
  0: { text: '待判题', color: 'default' },
  1: { text: '判题中', color: 'processing' },
  2: { text: 'AC', color: 'success' },
  3: { text: 'WA', color: 'error' },
  4: { text: 'TLE', color: 'warning' },
  5: { text: 'MLE', color: 'warning' },
  6: { text: 'RE', color: 'error' },
  7: { text: 'CE', color: 'error' },
};

const columns = [
  { title: '提交ID', dataIndex: 'id', width: 80 },
  { title: '题目', dataIndex: 'problemTitle' },
  { title: '语言', dataIndex: 'language', width: 100 },
  { title: '结果', dataIndex: 'result', width: 100 },
  { title: '耗时', key: 'timeCost', width: 100 },
  { title: '内存', key: 'memoryCost', width: 100 },
  { title: '通过率', key: 'passRate', width: 120 },
  { title: '提交时间', dataIndex: 'createdAt', width: 180 },
];

async function loadData() {
  loading.value = true;
  try {
    const res = await getMySubmissions(query.value);
    const data = res.data as any;
    submissions.value = data?.rows || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function handlePageChange(page: number) {
  query.value.pageNum = page;
  loadData();
}

onMounted(loadData);
</script>

<template>
  <div style="max-width: 1200px; margin: 0 auto">
    <a-card title="我的提交记录" :bordered="false">
      <a-table
        :columns="columns"
        :data-source="submissions"
        :loading="loading"
        :pagination="{ current: query.pageNum, pageSize: query.pageSize, total, onChange: handlePageChange }"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'result'">
            <a-tag :color="resultMap[record.result]?.color">
              {{ resultMap[record.result]?.text }}
            </a-tag>
          </template>
          <template v-if="column.dataIndex === 'language'">
            {{ record.language === 'java' ? 'Java' : 'Python' }}
          </template>
          <template v-if="column.key === 'timeCost'">
            {{ record.timeCost }}ms
          </template>
          <template v-if="column.key === 'memoryCost'">
            {{ record.memoryCost }}MB
          </template>
          <template v-if="column.key === 'passRate'">
            {{ record.passCount }}/{{ record.totalCount }}
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
