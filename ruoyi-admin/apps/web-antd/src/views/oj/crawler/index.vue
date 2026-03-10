<script setup lang="ts">
import type { CrawlerResultVO } from '#/api/oj/crawler/model';

import { ref, reactive } from 'vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Form,
  FormItem,
  InputNumber,
  Button,
  Spin,
  Alert,
  Divider,
  Row,
  Col,
  Statistic,
  Table,
  Space,
  message,
} from 'ant-design-vue';
import {
  CheckCircleOutlined,
  MinusCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons-vue';

import { executeCrawler } from '#/api/oj/crawler';

// 表单状态
const formState = reactive({
  limit: 20,
});

// 爬取状态
const crawling = ref(false);
const result = ref<CrawlerResultVO | null>(null);

// 失败题目列表表格列定义
const failedColumns = [
  {
    title: '题目标题',
    dataIndex: 'title',
    key: 'title',
  },
  {
    title: '失败原因',
    dataIndex: 'reason',
    key: 'reason',
  },
];

// 处理爬取操作
async function handleCrawl() {
  if (formState.limit < 1 || formState.limit > 100) {
    message.error('爬取数量必须在 1-100 之间');
    return;
  }

  crawling.value = true;
  result.value = null;

  try {
    const res = await executeCrawler({ limit: formState.limit });
    result.value = res;

    if (res.successCount > 0) {
      message.success(`成功导入 ${res.successCount} 道题目`);
    } else if (res.skipCount > 0) {
      message.warning(`所有题目均已存在，跳过 ${res.skipCount} 道题目`);
    } else {
      message.warning('未导入任何题目');
    }
  } catch (error: any) {
    message.error('爬取失败: ' + (error.message || '未知错误'));
  } finally {
    crawling.value = false;
  }
}
</script>

<template>
  <Page :auto-content-height="true">
    <Card title="LeetCode 题目爬虫" :bordered="false">
      <!-- 爬取表单 -->
      <Form layout="inline" :model="formState">
        <FormItem label="爬取数量">
          <InputNumber
            v-model:value="formState.limit"
            :min="1"
            :max="100"
            :disabled="crawling"
            placeholder="请输入爬取数量"
            style="width: 200px"
          />
        </FormItem>
        <FormItem>
          <Button
            type="primary"
            :loading="crawling"
            @click="handleCrawl"
          >
            {{ crawling ? '爬取中...' : '开始爬取' }}
          </Button>
        </FormItem>
      </Form>

      <!-- 进度显示 -->
      <div v-if="crawling" style="margin-top: 24px">
        <Spin tip="正在爬取题目数据，请稍候...">
          <Alert
            message="爬取进行中"
            description="正在从 LeetCode 获取题目数据并导入到题库"
            type="info"
          />
        </Spin>
      </div>

      <!-- 结果展示 -->
      <div v-if="result" style="margin-top: 24px">
        <Divider>爬取结果</Divider>

        <Row :gutter="16">
          <Col :span="8">
            <Statistic
              title="成功导入"
              :value="result.successCount"
              :value-style="{ color: '#3f8600' }"
            >
              <template #prefix>
                <CheckCircleOutlined />
              </template>
            </Statistic>
          </Col>
          <Col :span="8">
            <Statistic
              title="跳过（重复）"
              :value="result.skipCount"
              :value-style="{ color: '#faad14' }"
            >
              <template #prefix>
                <MinusCircleOutlined />
              </template>
            </Statistic>
          </Col>
          <Col :span="8">
            <Statistic
              title="失败"
              :value="result.failCount"
              :value-style="{ color: '#cf1322' }"
            >
              <template #prefix>
                <CloseCircleOutlined />
              </template>
            </Statistic>
          </Col>
        </Row>

        <!-- 失败题目列表 -->
        <div v-if="result.failedProblems && result.failedProblems.length > 0">
          <Divider>失败题目详情</Divider>
          <Table
            :columns="failedColumns"
            :data-source="result.failedProblems"
            :pagination="false"
            size="small"
          />
        </div>
      </div>
    </Card>
  </Page>
</template>
