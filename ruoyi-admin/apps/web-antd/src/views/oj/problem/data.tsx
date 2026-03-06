import type { FormSchemaGetter } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { DictEnum } from '@vben/constants';
import { getPopupContainer } from '@vben/utils';

import { categoryList } from '#/api/oj/category';
import { getDictOptions } from '#/utils/dict';
import { renderDict } from '#/utils/render';

export const querySchema: FormSchemaGetter = () => [
  {
    component: 'Select',
    componentProps: {
      getPopupContainer,
      options: [] as any[],
    },
    fieldName: 'categoryId',
    label: '题目分类',
    // 分类下拉数据通过 categoryList() 接口异步加载
    modelPropName: 'value',
  },
  {
    component: 'Input',
    fieldName: 'title',
    label: '题目标题',
  },
  {
    component: 'Select',
    componentProps: {
      getPopupContainer,
      options: getDictOptions(DictEnum.AC_DIFFICULTY),
    },
    fieldName: 'difficulty',
    label: '难度',
  },
  {
    component: 'Select',
    componentProps: {
      getPopupContainer,
      options: getDictOptions(DictEnum.SYS_NORMAL_DISABLE),
    },
    fieldName: 'status',
    label: '状态',
  },
];

/**
 * 异步加载分类下拉选项
 * 在页面组件中调用此函数，将返回值设置到 querySchema 的 categoryId 字段
 */
export async function loadCategoryOptions() {
  const res = await categoryList();
  return (res || []).map((item) => ({
    label: item.name,
    value: item.id,
  }));
}

export const columns: VxeGridProps['columns'] = [
  { type: 'checkbox', width: 60 },
  {
    title: '题目ID',
    field: 'id',
    width: 100,
  },
  {
    title: '题目标题',
    field: 'title',
  },
  {
    title: '难度',
    field: 'difficulty',
    width: 100,
    slots: {
      default: ({ row }) => {
        return renderDict(row.difficulty, DictEnum.AC_DIFFICULTY);
      },
    },
  },
  {
    title: '时间限制(ms)',
    field: 'timeLimit',
    width: 120,
  },
  {
    title: '内存限制(MB)',
    field: 'memoryLimit',
    width: 120,
  },
  {
    title: '提交次数',
    field: 'submitCount',
    width: 100,
  },
  {
    title: '通过次数',
    field: 'acCount',
    width: 100,
  },
  {
    title: '状态',
    field: 'status',
    width: 100,
    slots: {
      default: ({ row }) => {
        return renderDict(row.status, DictEnum.SYS_NORMAL_DISABLE);
      },
    },
  },
  {
    title: '创建时间',
    field: 'createdAt',
  },
  {
    field: 'action',
    fixed: 'right',
    slots: { default: 'action' },
    title: '操作',
    width: 200,
  },
];
