import { contextBridge, ipcRenderer } from 'electron'

// Expose protected methods that allow the renderer process to use
// the ipcRenderer without exposing the entire object
const api = {
  // 编译代码
  compileCode: (code: string) => ipcRenderer.invoke('compile-code', code),

  // 文件操作
  openFile: () => ipcRenderer.invoke('open-file'),
  saveFile: (path: string, content: string) => ipcRenderer.invoke('save-file', path, content),

  // 平台信息
  platform: process.platform,

  // 版本信息
  versions: {
    node: process.versions.node,
    chrome: process.versions.chrome,
    electron: process.versions.electron
  }
}

// Use `contextBridge` APIs to expose Electron APIs to renderer
contextBridge.exposeInMainWorld('electronAPI', api)