import { create } from 'zustand'
import { api, File, CompileResult } from '../services/api'

interface ProjectState {
  currentProject: {
    id: string
    name: string
    files: File[]
  } | null
  currentFile: File | null
  loading: boolean
  error: string | null

  loadProject: (id: string) => Promise<void>
  setCurrentFile: (file: File) => void
  updateFileContent: (content: string) => void
  compile: () => Promise<CompileResult>
}

export const useProjectStore = create<ProjectState>((set, get) => ({
  currentProject: null,
  currentFile: null,
  loading: false,
  error: null,

  loadProject: async (id: string) => {
    set({ loading: true, error: null })
    try {
      const project = await api.getProject(id)
      set({
        currentProject: project,
        currentFile: project.files[0] || null,
        loading: false
      })
    } catch (error) {
      set({ error: 'Failed to load project', loading: false })
    }
  },

  setCurrentFile: (file: File) => {
    set({ currentFile: file })
  },

  updateFileContent: (content: string) => {
    const { currentFile, currentProject } = get()
    if (currentFile && currentProject) {
      const updatedFile = { ...currentFile, content }
      set({ currentFile: updatedFile })

      // 更新项目文件列表
      const updatedFiles = currentProject.files.map(f =>
        f.id === currentFile.id ? updatedFile : f
      )
      set({
        currentProject: { ...currentProject, files: updatedFiles }
      })
    }
  },

  compile: async () => {
    const { currentFile, currentProject } = get()
    if (!currentFile || !currentProject) {
      throw new Error('No file to compile')
    }

    const result = await api.compile(
      currentFile.content || '',
      currentFile.name
    )

    return result
  }
}))