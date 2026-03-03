import { app, shell, BrowserWindow, ipcMain } from 'electron'
import { join } from 'path'
import { electronApp, optimizer, is } from '@electron-toolkit/utils'

function createWindow(): void {
  // Create the browser window.
  const mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1000,
    minHeight: 700,
    show: false,
    autoHideMenuBar: true,
    frame: true,
    titleBarStyle: 'default',
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false,
      nodeIntegration: false,
      contextIsolation: true
    }
  })

  mainWindow.on('ready-to-show', () => {
    mainWindow.show()
  })

  mainWindow.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url)
    return { action: 'deny' }
  })

  // HMR for renderer base on electron-vite cli.
  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    mainWindow.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    mainWindow.loadFile(join(__dirname, '../renderer/index.html'))
  }
}

// IPC handlers for compiler communication
ipcMain.handle('compile-code', async (_event, code: string) => {
  // 调用Kotlin编译器桥接
  // 这里会通过JNI或进程间通信调用编译器
  try {
    const result = await compileModelicaCode(code)
    return result
  } catch (error) {
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('open-file', async () => {
  const { dialog } = require('electron')
  const result = await dialog.showOpenDialog({
    filters: [{ name: 'Modelica Files', extensions: ['mo'] }],
    properties: ['openFile']
  })
  if (!result.canceled && result.filePaths.length > 0) {
    const fs = require('fs')
    const content = fs.readFileSync(result.filePaths[0], 'utf-8')
    return { path: result.filePaths[0], content }
  }
  return null
})

ipcMain.handle('save-file', async (_event, path: string, content: string) => {
  const fs = require('fs')
  fs.writeFileSync(path, content, 'utf-8')
  return { success: true }
})

// 编译器桥接函数（实际实现会调用Kotlin编译器）
async function compileModelicaCode(code: string): Promise<any> {
  // TODO: 实现与Kotlin编译器的桥接
  // 可以通过以下方式：
  // 1. 子进程调用Java/Kotlin编译器JAR
  // 2. 使用JNI直接调用
  // 3. 使用gRPC/HTTP调用编译服务

  return {
    success: true,
    message: 'Compilation successful',
    output: []
  }
}

// App lifecycle
app.whenReady().then(() => {
  // Set app user model id for windows
  electronApp.setAppUserModelId('org.modelica.ide')

  // Default open or close DevTools by F12 in development
  // and ignore CommandOrControl + R in production.
  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window)
  })

  createWindow()

  app.on('activate', function () {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

// Quit when all windows are closed, except on macOS.
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})