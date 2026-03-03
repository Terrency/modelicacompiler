import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface FileItem {
  path: string
  name: string
  content: string
}

interface OutputMessage {
  type: 'info' | 'success' | 'warning' | 'error'
  text: string
  timestamp: string
}

interface CompileError {
  message: string
  location?: {
    line: number
    column: number
  }
}

export const useProjectStore = defineStore('project', () => {
  // State
  const files = ref<FileItem[]>([])
  const openFiles = ref<FileItem[]>([])
  const currentFileIndex = ref<number>(-1)
  const outputMessages = ref<OutputMessage[]>([])
  const errors = ref<CompileError[]>([])

  // Computed
  const currentFile = computed(() =>
    currentFileIndex.value >= 0 ? openFiles.value[currentFileIndex.value] : null
  )

  // Actions
  function addFile(file: FileItem) {
    // 检查是否已存在
    const existing = files.value.find(f => f.path === file.path)
    if (!existing) {
      files.value.push(file)
    }
    openFile(file)
  }

  function openFile(file: FileItem) {
    // 检查是否已打开
    const index = openFiles.value.findIndex(f => f.path === file.path)
    if (index >= 0) {
      currentFileIndex.value = index
    } else {
      openFiles.value.push(file)
      currentFileIndex.value = openFiles.value.length - 1
    }
  }

  function closeFile(file: FileItem) {
    const index = openFiles.value.findIndex(f => f.path === file.path)
    if (index >= 0) {
      openFiles.value.splice(index, 1)
      if (currentFileIndex.value >= openFiles.value.length) {
        currentFileIndex.value = openFiles.value.length - 1
      }
    }
  }

  function setCurrentFile(file: FileItem) {
    const index = openFiles.value.findIndex(f => f.path === file.path)
    if (index >= 0) {
      currentFileIndex.value = index
    }
  }

  function updateFileContent(path: string, content: string) {
    const file = openFiles.value.find(f => f.path === path)
    if (file) {
      file.content = content
    }
    const originalFile = files.value.find(f => f.path === path)
    if (originalFile) {
      originalFile.content = content
    }
  }

  function createFile() {
    const newFile: FileItem = {
      path: `untitled-${Date.now()}.mo`,
      name: `untitled-${files.value.length + 1}.mo`,
      content: `model NewModel
  // Add your model here
equation
  // Add equations here
end NewModel;
`
    }
    files.value.push(newFile)
    openFile(newFile)
  }

  function addOutput(text: string, type: OutputMessage['type'] = 'info') {
    const timestamp = new Date().toLocaleTimeString()
    outputMessages.value.push({ type, text, timestamp })
  }

  function addError(message: string, location?: { line: number; column: number }) {
    errors.value.push({ message, location })
  }

  function clearOutput() {
    outputMessages.value = []
  }

  function clearErrors() {
    errors.value = []
  }

  return {
    files,
    openFiles,
    currentFile,
    outputMessages,
    errors,
    addFile,
    openFile,
    closeFile,
    setCurrentFile,
    updateFileContent,
    createFile,
    addOutput,
    addError,
    clearOutput,
    clearErrors
  }
})