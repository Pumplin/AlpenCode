<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, shallowRef } from 'vue';
import * as monaco from 'monaco-editor';

const props = defineProps<{
  modelValue: string;
  language?: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const editorRef = ref<HTMLDivElement>();
const editor = shallowRef<monaco.editor.IStandaloneCodeEditor>();

const languageMap: Record<string, string> = {
  java: 'java',
  python: 'python',
};

onMounted(() => {
  if (!editorRef.value) return;

  // 定义与页面统一的深色主题
  monaco.editor.defineTheme('alpencode-dark', {
    base: 'vs-dark',
    inherit: true,
    rules: [],
    colors: {
      'editor.background': '#0d1117',
    },
  });

  editor.value = monaco.editor.create(editorRef.value, {
    value: props.modelValue,
    language: languageMap[props.language || 'java'] || 'java',
    theme: 'alpencode-dark',
    fontSize: 14,
    minimap: { enabled: false },
    automaticLayout: true,
    scrollBeyondLastLine: false,
    tabSize: 4,
  });

  editor.value.onDidChangeModelContent(() => {
    const val = editor.value?.getValue() || '';
    emit('update:modelValue', val);
  });
});

watch(() => props.language, (lang) => {
  if (editor.value) {
    const model = editor.value.getModel();
    if (model) {
      monaco.editor.setModelLanguage(model, languageMap[lang || 'java'] || 'java');
    }
  }
});

watch(() => props.modelValue, (val) => {
  if (editor.value && val !== editor.value.getValue()) {
    editor.value.setValue(val);
  }
});

onBeforeUnmount(() => {
  editor.value?.dispose();
});
</script>

<template>
  <div ref="editorRef" style="width: 100%; height: 100%"></div>
</template>
