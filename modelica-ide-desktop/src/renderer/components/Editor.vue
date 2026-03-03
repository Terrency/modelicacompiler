<template>
  <div class="editor-wrapper">
    <div ref="editorContainer" class="monaco-container"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as monaco from 'monaco-editor'

// Props
const props = defineProps<{
  value: string
}>()

// Emits
const emit = defineEmits<{
  'update:value': [value: string]
}>()

// Refs
const editorContainer = ref<HTMLDivElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null

// Modelica语言定义
const modelicaLanguageConfig: monaco.languages.LanguageConfiguration = {
  comments: {
    lineComment: '//',
    blockComment: ['/*', '*/']
  },
  brackets: [
    ['{', '}'],
    ['[', ']'],
    ['(', ')']
  ],
  autoClosingPairs: [
    { open: '{', close: '}' },
    { open: '[', close: ']' },
    { open: '(', close: ')' },
    { open: '"', close: '"' }
  ],
  surroundingPairs: [
    { open: '{', close: '}' },
    { open: '[', close: ']' },
    { open: '(', close: ')' },
    { open: '"', close: '"' }
  ]
}

// Modelica语法高亮定义
const modelicaTokenProvider: monaco.languages.IMonarchLanguage = {
  defaultToken: '',
  tokenPostfix: '.modelica',

  keywords: [
    'algorithm', 'and', 'annotation', 'block', 'break', 'class', 'connect', 'connector',
    'constant', 'constrainedby', 'der', 'discrete', 'each', 'else', 'elseif', 'elsewhen',
    'encapsulated', 'end', 'enumeration', 'equation', 'expandable', 'extends', 'external',
    'false', 'final', 'flow', 'for', 'function', 'if', 'import', 'impure', 'in', 'inner',
    'input', 'loop', 'model', 'not', 'operator', 'or', 'outer', 'output', 'package',
    'parameter', 'partial', 'protected', 'public', 'pure', 'record', 'redeclare',
    'replaceable', 'return', 'stream', 'then', 'true', 'type', 'when', 'while', 'within'
  ],

  typeKeywords: [
    'Real', 'Integer', 'Boolean', 'String'
  ],

  operators: [
    '+', '-', '*', '/', '^',
    '=', ':=', '<>', '<', '<=', '>', '>=',
    'and', 'or', 'not'
  ],

  symbols: /[=><!~?:&|+\-*\/\^%]+/,

  tokenizer: {
    root: [
      // 标识符和关键字
      [/[a-zA-Z_]\w*/, {
        cases: {
          '@keywords': 'keyword',
          '@typeKeywords': 'type',
          '@default': 'identifier'
        }
      }],

      // 空白
      { include: '@whitespace' },

      // 分隔符
      [/[{}()\[\]]/, '@brackets'],
      [/[;,]/, 'delimiter'],

      // 数字
      [/\d*\.\d+([eE][\-+]?\d+)?/, 'number.float'],
      [/\d+/, 'number'],

      // 字符串
      [/"([^"\\]|\\.)*$/, 'string.invalid'],
      [/"/, { token: 'string.quote', bracket: '@open', next: '@string' }],

      // 操作符
      [/@symbols/, {
        cases: {
          '@operators': 'operator',
          '@default': ''
        }
      }]
    ],

    whitespace: [
      [/[ \t\r\n]+/, 'white'],
      [/\/\/.*$/, 'comment'],
      [/\/\*/, 'comment', '@comment']
    ],

    comment: [
      [/[^\/*]+/, 'comment'],
      [/\/\*/, 'comment', '@push'],
      ['\\*/', 'comment', '@pop'],
      [/[\/*]/, 'comment']
    ],

    string: [
      [/[^\\"]+/, 'string'],
      [/\\./, 'string.escape'],
      [/"/, { token: 'string.quote', bracket: '@close', next: '@pop' }]
    ]
  }
}

onMounted(() => {
  if (!editorContainer.value) return

  // 注册Modelica语言
  monaco.languages.register({ id: 'modelica' })
  monaco.languages.setLanguageConfiguration('modelica', modelicaLanguageConfig)
  monaco.languages.setMonarchTokensProvider('modelica', modelicaTokenProvider)

  // 设置主题
  monaco.editor.defineTheme('modelica-dark', {
    base: 'vs-dark',
    inherit: true,
    rules: [
      { token: 'keyword', foreground: '569CD6' },
      { token: 'type', foreground: '4EC9B0' },
      { token: 'number', foreground: 'B5CEA8' },
      { token: 'string', foreground: 'CE9178' },
      { token: 'comment', foreground: '6A9955' },
      { token: 'operator', foreground: 'D4D4D4' },
      { token: 'identifier', foreground: '9CDCFE' }
    ],
    colors: {
      'editor.background': '#1E1E1E'
    }
  })

  // 创建编辑器
  editor = monaco.editor.create(editorContainer.value, {
    value: props.value,
    language: 'modelica',
    theme: 'modelica-dark',
    fontSize: 14,
    fontFamily: 'Consolas, Monaco, monospace',
    minimap: { enabled: true },
    scrollBeyondLastLine: false,
    automaticLayout: true,
    tabSize: 2,
    wordWrap: 'on',
    lineNumbers: 'on',
    renderWhitespace: 'selection',
    folding: true,
    foldingHighlight: true,
    bracketPairColorization: { enabled: true }
  })

  // 监听内容变化
  editor.onDidChangeModelContent(() => {
    const value = editor?.getValue() || ''
    emit('update:value', value)
  })
})

onUnmounted(() => {
  editor?.dispose()
})

// 监听外部值变化
watch(() => props.value, (newValue) => {
  if (editor && editor.getValue() !== newValue) {
    editor.setValue(newValue)
  }
})
</script>

<style scoped>
.editor-wrapper {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.monaco-container {
  width: 100%;
  height: 100%;
}
</style>