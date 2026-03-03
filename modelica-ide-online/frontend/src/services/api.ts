import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

const client = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json'
  }
})

export interface Project {
  id: string
  name: string
  description?: string
  files: File[]
  createdAt: number
  updatedAt: number
}

export interface File {
  id: string
  name: string
  path: string
  content?: string
  size: number
  lastModified: number
}

export interface CompileResult {
  success: boolean
  errors: Array<{
    message: string
    line?: number
    column?: number
  }>
  warnings: Array<{
    message: string
    line?: number
    column?: number
  }>
  output: string[]
  generatedFiles: string[]
}

export const api = {
  // 项目管理
  async getProjects(): Promise<Project[]> {
    const response = await client.get('/projects')
    return response.data
  },

  async getProject(id: string): Promise<Project> {
    const response = await client.get(`/projects/${id}`)
    return response.data
  },

  async createProject(data: { name: string; description?: string }): Promise<Project> {
    const response = await client.post('/projects', data)
    return response.data
  },

  async updateProject(id: string, data: { name?: string; description?: string }): Promise<Project> {
    const response = await client.put(`/projects/${id}`, data)
    return response.data
  },

  async deleteProject(id: string): Promise<void> {
    await client.delete(`/projects/${id}`)
  },

  // 文件管理
  async getFile(projectId: string, fileId: string): Promise<File> {
    const response = await client.get(`/projects/${projectId}/files/${fileId}`)
    return response.data
  },

  async createFile(projectId: string, data: { name: string; path?: string; content?: string }): Promise<File> {
    const response = await client.post(`/projects/${projectId}/files`, data)
    return response.data
  },

  async updateFile(projectId: string, fileId: string, content: string): Promise<File> {
    const response = await client.put(`/projects/${projectId}/files/${fileId}`, { content })
    return response.data
  },

  async deleteFile(projectId: string, fileId: string): Promise<void> {
    await client.delete(`/projects/${projectId}/files/${fileId}`)
  },

  // 编译
  async compile(code: string, fileName?: string): Promise<CompileResult> {
    const response = await client.post('/compile', { code, fileName })
    return response.data
  }
}