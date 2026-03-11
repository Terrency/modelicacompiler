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
  const { spawn } = require('child_process')
  const path = require('path')

  return new Promise((resolve) => {
    const outputLines: string[] = []
    const errorLines: string[] = []

    // 添加编译开始日志
    outputLines.push('=== Modelica Compiler ===')
    outputLines.push(`[${new Date().toISOString()}] Starting compilation...`)
    outputLines.push(`[${new Date().toISOString()}] Code length: ${code.length} bytes`)
    outputLines.push('')

    // 获取 JAR 路径
    const jarPath = path.join(__dirname, '../../native/libs/native-1.0.0-SNAPSHOT.jar')

    // 检查 JAR 是否存在
    const fs = require('fs')
    if (!fs.existsSync(jarPath)) {
      outputLines.push(`[${new Date().toISOString()}] ERROR: Compiler JAR not found at: ${jarPath}`)
      resolve({
        success: false,
        error: 'Compiler JAR not found',
        output: outputLines
      })
      return
    }

    outputLines.push(`[${new Date().toISOString()}] Compiler JAR: ${jarPath}`)

    // 创建临时文件
    const os = require('os')
    const tempDir = os.tmpdir()
    const tempFile = path.join(tempDir, `modelica_temp_${Date.now()}.mo`)

    try {
      fs.writeFileSync(tempFile, code, 'utf-8')
      outputLines.push(`[${new Date().toISOString()}] Temp file: ${tempFile}`)
      outputLines.push(`[${new Date().toISOString()}] Invoking compiler...`)
      outputLines.push('')

      // 调用 Java 编译器 (使用 file 命令)
      const javaProcess = spawn('java', ['-jar', jarPath, 'file', tempFile], {
        stdio: ['pipe', 'pipe', 'pipe']
      })

    // 捕获标准输出
      javaProcess.stdout.on('data', (data: Buffer) => {
        const lines = data.toString().split('\n')
        lines.forEach((line: string) => {
          if (line.trim()) {
            outputLines.push(line)
          }
        })
      })

      // 捕获标准错误
      javaProcess.stderr.on('data', (data: Buffer) => {
        const lines = data.toString().split('\n')
        lines.forEach((line: string) => {
          if (line.trim()) {
            errorLines.push(line)
          }
        })
      })

      // 处理编译完成
      javaProcess.on('close', (code: number) => {
        // 清理临时文件
        try {
          fs.unlinkSync(tempFile)
        } catch (e) {
          // 忽略清理错误
        }

        outputLines.push('')
        outputLines.push(`[${new Date().toISOString()}] Compilation ${code === 0 ? 'succeeded' : 'failed'} (exit code: ${code})`)

        if (errorLines.length > 0) {
          outputLines.push('')
          outputLines.push('=== Compiler Errors ===')
          errorLines.forEach(line => outputLines.push(line))
        }

        resolve({
          success: code === 0,
          error: code !== 0 ? 'Compilation failed' : undefined,
          output: outputLines
        })
      })

      // 处理编译错误
      javaProcess.on('error', (error: Error) => {
        // 清理临时文件
        try {
          fs.unlinkSync(tempFile)
        } catch (e) {
          // 忽略清理错误
        }

        outputLines.push('')
        outputLines.push(`[${new Date().toISOString()}] ERROR: Failed to start compiler`)
        outputLines.push(`Error: ${error.message}`)
        outputLines.push('')
        outputLines.push('Make sure Java is installed and available in PATH')

        resolve({
          success: false,
          error: error.message,
          output: outputLines
        })
      })
    } catch (error: any) {
      outputLines.push('')
      outputLines.push(`[${new Date().toISOString()}] ERROR: Failed to create temp file`)
      outputLines.push(`Error: ${error.message}`)

      resolve({
        success: false,
        error: error.message,
        output: outputLines
      })
    }
  })
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