import type { FormSchemaGetter } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { DictEnum } from '@vben/constants';
import { getPopupContainer } from '@vben/utils';

import { getDictOptions } from '#/utils/dict';
import { renderDict } from '#/utils/render';

export const columns: VxeGridProps['columns'] = [
  { type: 'checkbox', width: 60 },
  {
    title: '用例ID',
    field: 'id',
    width: 100,
  },
  {
    title: '输入数据',
    field: 'input',
    showOverflow: true,
  },
  {
    title: '期望输出',
    field: 'expectedOutput',
    showOverflow: true,
  },
  {
    title: '是否公开样例',
    field: 'isSample',
    width: 130,
    slots: {
      default: ({ row }) => {
        return renderDict(row.isSample, DictEnum.AC_SAMPLE_TYPE);
      },
    },
  },
  {
    title: '排序',
    field: 'sort',
    width: 80,
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
    field: 'action',
    fixed: 'right',
    slots: { default: 'action' },
    title: '操作',
    width: 180,
  },
];

export const modalSchema: FormSchemaGetter = () => [
  {
    component: 'Input',
    dependencies: {
      show: () => false,
      triggerFields: [''],
    },
    fieldName: 'id',
    label: '主键',
  },
  {
    component: 'Textarea',
    fieldName: 'input',
    label: '输入数据',
    rules: 'required',
  },
  {
    component: 'Textarea',
    fieldName: 'expectedOutput',
    label: '期望输出',
    rules: 'required',
  },
  {
    component: 'RadioGroup',
    componentProps: {
      buttonStyle: 'solid',
      options: getDictOptions(DictEnum.AC_SAMPLE_TYPE),
      optionType: 'button',
    },
    defaultValue: '0',
    fieldName: 'isSample',
    label: '是否公开样例',
  },
  {
    component: 'InputNumber',
    defaultValue: 0,
    fieldName: 'sort',
    label: '排序',
  },
  {
    component: 'Select',
    componentProps: {
      getPopupContainer,
      options: getDictOptions(DictEnum.SYS_NORMAL_DISABLE),
    },
    defaultValue: '0',
    fieldName: 'status',
    label: '状态',
  },
];
