<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getProblemList, getCategoryList } from '@/api/problem';
import type { Problem, ProblemCategory } from '@/api/types';

const router = useRouter();
const loading = ref(false);
const problems = ref<Problem[]>([]);
const categories = ref<ProblemCategory[]>([]);
const total = ref(0);

const query = ref({
  pageNum: 1,
  pageSize: 20,
  categoryId: undefined as number | undefined,
  difficulty: undefined as number | undefined,
  title: '',
});

const difficultyMap: Record<number, { text: string; color: string }> = {
  1: { text: '简单', color: 'green' },
  2: { text: '中等', color: 'orange' },
  3: { text: '困难', color: 'red' },
};

const columns = [
  { title: '#', dataIndex: 'id', width: 80 },
  { title: '题目', dataIndex: 'title' },
  { title: '难度', dataIndex: 'difficulty', width: 100 },
  { title: '通过率', key: 'acRate', width: 120 },
];

async function loadData() {
  loading.value = true;
  try {
    const res = await getProblemList(query.value);
    const data = res.data as any;
    problems.value = data?.rows || [];
    total.value = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

async function loadCategories() {
  try {
    const res = await getCategoryList();
    categories.value = res.data || res || [];
  } catch { /* ignore */ }
}

function handlePageChange(page: number) {
  query.value.pageNum = page;
  loadData();
}

function goToProblem(id: number) {
  router.push(`/problem/${id}`);
}

function getAcRate(row: Problem) {
  if (!row.submitCount) return '0%';
  return ((row.acCount / row.submitCount) * 100).toFixed(1) + '%';
}

onMounted(() => {
  loadData();
  loadCategories();
});
</script>

<template>
  <div style="max-width: 1200px; margin: 0 auto">
    <!-- 筛选栏 -->
    <a-card :bordered="false" style="margin-bottom: 16px">
      <a-space wrap>
        <a-input-search
          v-model:value="query.title"
          placeholder="搜索题目"
          style="width: 240px"
          allow-clear
          @search="loadData"
        />
        <a-select
          v-model:value="query.difficulty"
          placeholder="难度"
          allow-clear
          style="width: 120px"
          @change="loadData"
        >
          <a-select-option :value="1">简单</a-select-option>
          <a-select-option :value="2">中等</a-select-option>
          <a-select-option :value="3">困难</a-select-option>
        </a-select>
        <a-select
          v-model:value="query.categoryId"
          placeholder="分类"
          allow-clear
          style="width: 140px"
          @change="loadData"
        >
          <a-select-option v-for="cat in categories" :key="cat.id" :value="cat.id">
            {{ cat.name }}
          </a-select-option>
        </a-select>
      </a-space>
    </a-card>

    <!-- 题目列表 -->
    <a-card :bordered="false">
      <a-table
        :columns="columns"
        :data-source="problems"
        :loading="loading"
        :pagination="{ current: query.pageNum, pageSize: query.pageSize, total, onChange: handlePageChange }"
        row-key="id"
        :custom-row="(record: Problem) => ({ onClick: () => goToProblem(record.id), style: { cursor: 'pointer' } })"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'difficulty'">
            <a-tag :color="difficultyMap[record.difficulty]?.color">
              {{ difficultyMap[record.difficulty]?.text }}
            </a-tag>
          </template>
          <template v-if="column.key === 'acRate'">
            {{ getAcRate(record) }}
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>
