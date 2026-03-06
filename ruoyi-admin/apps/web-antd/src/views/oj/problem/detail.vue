<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { Problem } from '#/api/oj/problem/model';
import type { TestCase } from '#/api/oj/testCase/model';

import { computed, onMounted, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { DictEnum } from '@vben/constants';
import { getVxePopupContainer } from '@vben/utils';

import {
  Button,
  Card,
  Descriptions,
  DescriptionsItem,
  Popconfirm,
  Space,
  Tag,
} from 'ant-design-vue';
import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/preview.css';
import { useRoute, useRouter } from 'vue-router';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { problemInfo } from '#/api/oj/problem';
import { testCasePage, testCaseRemove } from '#/api/oj/testCase';
import { renderDict } from '#/utils/render';

import { columns } from '../testCase/data';
import TestCaseModal from '../testCase/testCase-modal.vue';

const route = useRoute();
const router = useRouter();
const problemId = computed(() => Number(route.query.id));

const problem = ref<Problem>();
const loading = ref(false);



// 测试用例表格配置
const gridOptions: VxeGridProps = {
  checkboxConfig: {
    highlight: true,
    reserve: true,
  },
  columns,
  height: 400,
  keepSource: true,
  pagerConfig: {},
  proxyConfig: {
    ajax: {
      query: async ({ page }) => {
        return await testCasePage({
          problemId: problemId.value,
          pageNum: page.currentPage,
          pageSize: page.pageSize,
        });
      },
    },
  },
  rowConfig: {
    keyField: 'id',
  },
  id: 'oj-problem-detail-testcase',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ gridOptions });

const [TestCaseModalComp, modalApi] = useVbenModal({
  connectedComponent: TestCaseModal,
});

function handleAddTestCase() {
  modalApi.setData({ problemId: problemId.value });
  modalApi.open();
}

function handleEditTestCase(row: TestCase) {
  modalApi.setData({ id: row.id, problemId: problemId.value });
  modalApi.open();
}

async function handleDeleteTestCase(row: TestCase) {
  await testCaseRemove([row.id]);
  await tableApi.query();
}

function handleBack() {
  router.back();
}

async function loadProblem() {
  if (!problemId.value) return;
  loading.value = true;
  try {
    problem.value = await problemInfo(problemId.value);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadProblem();
});
</script>

<template>
  <Page>
    <div style="display: flex; flex-direction: column; gap: 16px">
      <!-- 返回按钮 -->
      <div>
        <Button @click="handleBack">← 返回</Button>
      </div>

      <!-- 题目信息卡片 -->
      <Card title="题目信息" :loading="loading">
        <Descriptions v-if="problem" bordered :column="2">
          <DescriptionsItem label="题目标题" :span="2">
            {{ problem.title }}
          </DescriptionsItem>
          <DescriptionsItem label="难度">
            <component :is="() => renderDict(problem!.difficulty, DictEnum.AC_DIFFICULTY)" />
          </DescriptionsItem>
          <DescriptionsItem label="状态">
            <component :is="() => renderDict(problem!.status, DictEnum.SYS_NORMAL_DISABLE)" />
          </DescriptionsItem>
          <DescriptionsItem label="时间限制">
            {{ problem.timeLimit }} ms
          </DescriptionsItem>
          <DescriptionsItem label="内存限制">
            {{ problem.memoryLimit }} MB
          </DescriptionsItem>
          <DescriptionsItem label="提交次数">
            {{ problem.submitCount }}
          </DescriptionsItem>
          <DescriptionsItem label="通过次数">
            {{ problem.acCount }}
          </DescriptionsItem>
          <DescriptionsItem label="关联分类" :span="2">
            <Space>
              <Tag v-for="cat in problem.categories" :key="cat.id" color="blue">
                {{ cat.name }}
              </Tag>
              <span v-if="!problem.categories?.length">无</span>
            </Space>
          </DescriptionsItem>
          <DescriptionsItem label="创建时间" :span="2">
            {{ problem.createdAt }}
          </DescriptionsItem>
          <DescriptionsItem label="题目描述" :span="2">
            <MdPreview :model-value="problem.description || ''" />
          </DescriptionsItem>
        </Descriptions>
      </Card>

      <!-- 测试用例表格 -->
      <Card title="测试用例">
        <BasicTable table-title="">
          <template #toolbar-tools>
            <Button type="primary" @click="handleAddTestCase">
              新增用例
            </Button>
          </template>
          <template #action="{ row }">
            <Space>
              <ghost-button @click="handleEditTestCase(row)">
                编辑
              </ghost-button>
              <Popconfirm
                :get-popup-container="getVxePopupContainer"
                placement="left"
                title="确认删除？"
                @confirm="handleDeleteTestCase(row)"
              >
                <ghost-button danger @click.stop="">
                  删除
                </ghost-button>
              </Popconfirm>
            </Space>
          </template>
        </BasicTable>
        <TestCaseModalComp @reload="tableApi.query()" />
      </Card>
    </div>
  </Page>
</template>
