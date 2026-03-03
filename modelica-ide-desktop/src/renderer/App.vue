<template>
  <div class="app-container">
    <!-- 顶部工具栏 -->
    <header class="toolbar">
      <div class="toolbar-left">
        <button @click="openFile" class="toolbar-btn" title="Open File">
          <span class="icon">📂</span> Open
        </button>
        <button @click="saveFile" class="toolbar-btn" title="Save File">
          <span class="icon">💾</span> Save
        </button>
        <div class="separator"></div>
        <button @click="compile" class="toolbar-btn primary" title="Compile">
          <span class="icon">▶</span> Compile
        </button>
      </div>
      <div class="toolbar-right">
        <span class="status">{{ statusText }}</span>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 左侧文件树 -->
      <aside class="sidebar">
        <div class="sidebar-header">
          <h3>Files</h3>
        </div>
        <div class="file-tree">
          <div
            v-for="file in files"
            :key="file.path"
            class="file-item"
            :class="{ active: currentFile?.path === file.path }"
            @click="selectFile(file)"
          >
            <span class="file-icon">📄</span>
            {{ file.name }}
          </div>
        </div>
      </aside>

      <!-- 编辑器区域 -->
      <div class="editor-container">
        <div class="editor-tabs">
          <div
            v-for="file in openFiles"
            :key="file.path"
            class="tab"
            :class="{ active: currentFile?.path === file.path }"
            @click="selectFile(file)"
          >
            {{ file.name }}
            <button class="tab-close" @click.stop="closeFile(file)">×</button>
          </div>
        </div>
        <Editor
          v-if="currentFile"
          :value="currentFile.content"
          @update:value="updateContent"
        />
        <div v-else class="welcome-screen">
          <h2>Welcome to Modelica IDE</h2>
          <p>Open a file or create a new one to get started.</p>
          <button @click="newFile" class="primary-btn">New File</button>
        </div>
      </div>

      <!-- 右侧面板 -->
      <aside class="right-panel">
        <div class="panel-tabs">
          <button
            :class="{ active: rightPanelTab === 'output' }"
            @click="rightPanelTab = 'output'"
          >
            Output
          </button>
          <button
            :class="{ active: rightPanelTab === 'errors' }"
            @click="rightPanelTab = 'errors'"
          >
            Errors
          </button>
        </div>
        <div class="panel-content">
          <OutputConsole v-if="rightPanelTab === 'output'" :messages="outputMessages" />
          <ErrorPanel v-else :errors="errors" />
        </div>
      </aside>
    </main>

    <!-- 底部状态栏 -->
    <footer class="status-bar">
      <span>{{ currentFile?.path || 'No file open' }}</span>
      <span v-if="currentFile">Ln {{ cursorPosition.line }}, Col {{ cursorPosition.column }}</span>
      <span>Modelica IDE v1.0.0</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useProjectStore } from './stores/project'
import Editor from './components/Editor.vue'
import OutputConsole from './components/OutputPanel.vue'
import ErrorPanel from './components/ErrorList.vue'

const store = useProjectStore()

// 状态
const rightPanelTab = ref<'output' | 'errors'>('output')
const statusText = ref('Ready')
const cursorPosition = ref({ line: 1, column: 1 })

// 计算属性
const files = computed(() => store.files)
const openFiles = computed(() => store.openFiles)
const currentFile = computed(() => store.currentFile)
const outputMessages = computed(() => store.outputMessages)
const errors = computed(() => store.errors)

// 方法
async function openFile() {
  try {
    const result = await window.electronAPI.openFile()
    if (result) {
      store.addFile({
        path: result.path,
        name: result.path.split('/').pop() || result.path,
        content: result.content
      })
    }
  } catch (error) {
    console.error('Failed to open file:', error)
  }
}

async function saveFile() {
  if (currentFile.value) {
    await window.electronAPI.saveFile(currentFile.value.path, currentFile.value.content)
    statusText.value = 'File saved'
  }
}

async function compile() {
  if (!currentFile.value) return

  statusText.value = 'Compiling...'
  store.clearOutput()
  store.clearErrors()

  try {
    const result = await window.electronAPI.compileCode(currentFile.value.content)

    if (result.success) {
      store.addOutput('Compilation successful!')
      statusText.value = 'Compilation successful'
    } else {
      store.addError(result.error || 'Compilation failed')
      statusText.value = 'Compilation failed'
    }
  } catch (error) {
    store.addError(String(error))
    statusText.value = 'Compilation error'
  }
}

function newFile() {
  store.createFile()
}

function selectFile(file: any) {
  store.setCurrentFile(file)
}

function closeFile(file: any) {
  store.closeFile(file)
}

function updateContent(content: string) {
  if (currentFile.value) {
    store.updateFileContent(currentFile.value.path, content)
  }
}
</script>