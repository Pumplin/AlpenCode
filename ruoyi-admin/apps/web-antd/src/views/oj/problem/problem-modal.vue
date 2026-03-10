<script setup lang="ts">
import type { RuleObject } from 'ant-design-vue/es/form';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { DictEnum } from '@vben/constants';
import { $t } from '@vben/locales';
import { cloneDeep } from '@vben/utils';

import { Form, FormItem, Input, InputNumber, Select } from 'ant-design-vue';
import { pick } from 'lodash-es';

import { categoryList } from '#/api/oj/category';
import { problemAdd, problemInfo, problemUpdate } from '#/api/oj/problem';
import { MarkdownEditor } from '#/components/markdown';
import { getDictOptions } from '#/utils/dict';

const emit = defineEmits<{ reload: [] }>();

const isUpdate = ref(false);
const title = computed(() => {
  return isUpdate.value ? $t('pages.common.edit') : $t('pages.common.add');
});

interface FormData {
  id?: number;
  title?: string;
  description?: string;
  difficulty?: number | string;
  timeLimit?: number;
  memoryLimit?: number;
  status?: number | string;
  categoryIds?: number[];
}

const defaultValues: FormData = {
  id: undefined,
  title: '',
  description: '',
  difficulty: undefined,
  timeLimit: 1000,
  memoryLimit: 256,
  status: 0,
  categoryIds: [],
};

const formData = ref<FormData>({ ...defaultValues });

const categoryOptions = ref<{ label: string; value: number }[]>([]);

type AntdFormRules<T> = Partial<Record<keyof T, RuleObject[]>> & {
  [key: string]: RuleObject[];
};

const formRules = ref<AntdFormRules<FormData>>({
  title: [{ required: true, message: $t('ui.formRules.required') }],
  description: [{ required: true, message: $t('ui.formRules.required') }],
  difficulty: [{ required: true, message: $t('ui.formRules.selectRequired') }],
});

const { validate, validateInfos, resetFields } = Form.useForm(
  formData,
  formRules,
);

async function loadCategoryOptions() {
  try {
    const list = await categoryList();
    categoryOptions.value = (list || []).map((item) => ({
      label: item.name,
      value: item.id,
    }));
  } catch {
    categoryOptions.value = [];
  }
}

const [BasicModal, modalApi] = useVbenModal({
  class: 'w-[800px]',
  fullscreenButton: true,
  closeOnClickModal: false,
  onClosed: handleCancel,
  onConfirm: handleConfirm,
  onOpenChange: async (isOpen) => {
    if (!isOpen) {
      return null;
    }
    modalApi.modalLoading(true);

    await loadCategoryOptions();

    const { id } = modalApi.getData() as { id?: number | string };
    isUpdate.value = !!id;
    if (isUpdate.value && id) {
      const record = await problemInfo(id);
      const filterRecord = pick(record, Object.keys(defaultValues));
      // 从 categories 数组中提取 categoryIds
      if (record.categories && record.categories.length > 0) {
        filterRecord.categoryIds = record.categories.map((c) => c.id);
      }
      formData.value = filterRecord;
    }
    modalApi.modalLoading(false);
  },
});

async function handleConfirm() {
  try {
    modalApi.modalLoading(true);
    await validate();
    const data = cloneDeep(formData.value);
    await (isUpdate.value ? problemUpdate(data) : problemAdd(data));
    emit('reload');
    await handleCancel();
  } catch (error) {
    console.error(error);
  } finally {
    modalApi.modalLoading(false);
  }
}

async function handleCancel() {
  modalApi.close();
  formData.value = { ...defaultValues, categoryIds: [] };
  resetFields();
}
</script>

<template>
  <BasicModal :title="title">
    <Form layout="vertical">
      <FormItem label="题目标题" v-bind="validateInfos.title">
        <Input
          :placeholder="$t('ui.formRules.required')"
          v-model:value="formData.title"
        />
      </FormItem>
      <FormItem label="题目描述" v-bind="validateInfos.description">
        <MarkdownEditor v-model:model-value="formData.description" />
      </FormItem>
      <div class="grid lg:grid-cols-2 sm:grid-cols-1">
        <FormItem label="难度" v-bind="validateInfos.difficulty">
          <Select
            :options="getDictOptions(DictEnum.AC_DIFFICULTY, true)"
            :placeholder="$t('ui.formRules.selectRequired')"
            v-model:value="formData.difficulty"
          />
        </FormItem>
        <FormItem label="状态">
          <Select
            :options="getDictOptions(DictEnum.SYS_NORMAL_DISABLE, true)"
            :placeholder="$t('ui.formRules.selectRequired')"
            v-model:value="formData.status"
          />
        </FormItem>
      </div>
      <div class="grid lg:grid-cols-2 sm:grid-cols-1">
        <FormItem label="时间限制(ms)">
          <InputNumber
            :min="100"
            :style="{ width: '100%' }"
            v-model:value="formData.timeLimit"
          />
        </FormItem>
        <FormItem label="内存限制(MB)">
          <InputNumber
            :min="16"
            :style="{ width: '100%' }"
            v-model:value="formData.memoryLimit"
          />
        </FormItem>
      </div>
      <FormItem label="题目分类">
        <Select
          :options="categoryOptions"
          mode="multiple"
          placeholder="请选择题目分类"
          v-model:value="formData.categoryIds"
        />
      </FormItem>
    </Form>
  </BasicModal>
</template>
