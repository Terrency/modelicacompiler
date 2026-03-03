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
        <!-- Modelica Standard Library -->
        <div class="sidebar-section">
          <div class="sidebar-header" @click="toggleLibrary">
            <span class="toggle-icon">{{ libraryExpanded ? '▼' : '▶' }}</span>
            <h3>📚 Modelica Standard Library</h3>
          </div>
          <div v-show="libraryExpanded" class="file-tree">
            <div
              v-for="file in libraryFiles"
              :key="file.path"
              class="file-item library-file"
              :class="{ active: currentFile?.path === file.path }"
              @click="selectFile(file)"
            >
              <span class="file-icon">📦</span>
              {{ file.name }}
            </div>
          </div>
        </div>

        <!-- Examples -->
        <div class="sidebar-section">
          <div class="sidebar-header" @click="toggleExamples">
            <span class="toggle-icon">{{ examplesExpanded ? '▼' : '▶' }}</span>
            <h3>📝 Examples</h3>
          </div>
          <div v-show="examplesExpanded" class="file-tree">
            <div
              v-for="file in exampleFiles"
              :key="file.path"
              class="file-item example-file"
              :class="{ active: currentFile?.path === file.path }"
              @click="selectFile(file)"
            >
              <span class="file-icon">🧪</span>
              {{ file.name }}
            </div>
          </div>
        </div>

        <!-- User Files -->
        <div class="sidebar-section" v-if="userFiles.length > 0">
          <div class="sidebar-header">
            <h3>📁 My Files</h3>
            <button class="new-file-btn" @click="newFile" title="New File">+</button>
          </div>
          <div class="file-tree">
            <div
              v-for="file in userFiles"
              :key="file.path"
              class="file-item user-file"
              :class="{ active: currentFile?.path === file.path }"
              @click="selectFile(file)"
            >
              <span class="file-icon">📄</span>
              {{ file.name }}
            </div>
          </div>
        </div>

        <!-- New File Button -->
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
            @click="selectFile(file)"
          >
            <span v-if="file.isLibrary" class="tab-icon">📦</span>
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
import OutputConsole from './components/OutputPanel.vue'
import ErrorPanel from './components/ErrorList.vue'

const store = useProjectStore()

// 状态
const rightPanelTab = ref<'output' | 'errors'>('output')
const statusText = ref('Ready')
const cursorPosition = ref({ line: 1, column: 1 })
const libraryExpanded = ref(true)
const examplesExpanded = ref(true)

// 初始化 - 加载MSL
onMounted(() => {
  store.loadStandardLibrary()
})

// 计算属性
const files = computed(() => store.files)
const openFiles = computed(() => store.openFiles)
const currentFile = computed(() => store.currentFile)
const outputMessages = computed(() => store.outputMessages)
const errors = computed(() => store.errors)
const libraryFiles = computed(() => store.libraryFiles.filter(f => !f.path.startsWith('Examples')))
const exampleFiles = computed(() => store.libraryFiles.filter(f => f.path.startsWith('Examples')))
const userFiles = computed(() => store.userFiles)

// 方法
function toggleLibrary() {
  libraryExpanded.value = !libraryExpanded.value
}

function toggleExamples() {
  examplesExpanded.value = !examplesExpanded.value
}

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

function openFirstExample() {
  const helloWorld = store.files.find(f => f.name === 'HelloWorld')
  if (helloWorld) {
    store.openFile(helloWorld)
  }
}
</script>