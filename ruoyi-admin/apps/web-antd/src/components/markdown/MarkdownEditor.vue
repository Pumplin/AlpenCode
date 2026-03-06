<script lang="ts" setup>
import { MdEditor } from 'md-editor-v3';
import 'md-editor-v3/lib/style.css';

defineOptions({ name: 'MarkdownEditor' });

const modelValue = defineModel<string>('modelValue', { default: '' });
</script>

<template>
  <MdEditor v-model="modelValue" />
</template>
