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
        <div class="sidebar-header-main">
          <h3>📦 Modelica Standard Library</h3>
        </div>

        <FileTree
          :nodes="treeNodes"
          :expanded-ids="expandedNodes"
          :active-path="currentFile?.path || null"
          @toggle="handleToggle"
          @select="handleSelect"
        />

        <!-- 新建文件按钮 -->
        <div class="sidebar-actions">
          <button class="toolbar-btn" @click="newFile" title="New File">
            <span class="icon">➕</span> New File
          </button>
        </div>
      </aside>

      <!-- 编辑器区域 -->
      <div class="editor-container">
        <div class="editor-tabs">
          <div
            v-for="file in openFiles"
            :key="file.path"
            class="tab"
            :class="{ active: currentFile?.path === file.path, 'library-tab': file.isLibrary }"
            @click="selectTab(file)"
          >
            <span v-if="file.isLibrary" class="tab-icon">📦</span>
            {{ file.name }}
            <button class="tab-close" @click.stop="closeFile(file)">×</button>
          </div>
        </div>
        <Editor
          v-if="currentFile"
          :value="currentFile.content || ''"
          @update:value="updateContent"
        />
        <div v-else class="welcome-screen">
          <h2>Welcome to Modelica IDE</h2>
          <p>Open a file from the library or create a new one to get started.</p>
          <div class="welcome-actions">
            <button @click="newFile" class="primary-btn">New File</button>
            <button @click="openFirstExample" class="secondary-btn">Open Example</button>
          </div>
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
      <span>Modelica IDE v1.0.0 | MSL 4.0.0</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useProjectStore } from './stores/project'
import Editor from './components/Editor.vue'
import FileTree from './components/FileTree.vue'
import OutputConsole from './components/OutputPanel.vue'
import ErrorPanel from './components/ErrorList.vue'

const store = useProjectStore()

// 状态
const rightPanelTab = ref<'output' | 'errors'>('output')
const statusText = ref('Ready')
const cursorPosition = ref({ line: 1, column: 1 })

// 初始化 - 加载MSL
onMounted(() => {
  store.loadStandardLibrary()
})

// 计算属性
const treeNodes = computed(() => store.treeNodes)
const openFiles = computed(() => store.openFiles)
const currentFile = computed(() => store.currentFile)
const outputMessages = computed(() => store.outputMessages)
const errors = computed(() => store.errors)
const expandedNodes = computed(() => store.expandedNodes)

// 树节点操作
function handleToggle(node: any) {
  store.toggleNode(node)
}

function handleSelect(node: any) {
  if (node.content) {
    store.openFile(node)
  }
}

// 文件操作
async function openFile() {
  try {
    const result = await window.electronAPI.openFile()
    if (result) {
      // 添加到树中
      const newNode = {
        id: result.path,
        name: result.path.split('/').pop() || result.path,
        type: 'class',
        path: result.path,
        content: result.content,
        children: [],
        isExpanded: false,
        isLibrary: false
      }
      store.treeNodes.push(newNode)
      store.openFile(newNode)
    }
  } catch (error) {
    console.error('Failed to open file:', error)
  }
}

async function saveFile() {
  if (currentFile.value) {
    await window.electronAPI.saveFile(currentFile.value.path, currentFile.value.content || '')
    statusText.value = 'File saved'
  }
}

async function compile() {
  if (!currentFile.value) return

  statusText.value = 'Compiling...'
  store.clearOutput()
  store.clearErrors()

  try {
    const result = await window.electronAPI.compileCode(currentFile.value.content || '')

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

function selectTab(file: any) {
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

function openFirstExample() {
  // 找到 HelloWorld 示例
  const findHelloWorld = (nodes: any[]): any => {
    for (const node of nodes) {
      if (node.name === 'HelloWorld') return node
      if (node.children?.length > 0) {
        const found = findHelloWorld(node.children)
        if (found) return found
      }
    }
    return null
  }

  const helloWorld = findHelloWorld(store.treeNodes)
  if (helloWorld) {
    store.openFile(helloWorld)
  }
}
</script>