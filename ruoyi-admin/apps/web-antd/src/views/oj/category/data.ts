import type { FormSchemaGetter } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

export const columns: VxeGridProps['columns'] = [
  { type: 'checkbox', width: 60 },
  {
    title: '分类ID',
    field: 'id',
    width: 100,
  },
  {
    title: '分类名称',
    field: 'name',
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
    component: 'Input',
    fieldName: 'name',
    label: '分类名称',
    rules: 'required',
  },
];
