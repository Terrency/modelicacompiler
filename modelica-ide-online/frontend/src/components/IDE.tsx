import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import * as monaco from 'monaco-editor'
import { useProjectStore } from '../stores/projectStore'
import { useWebSocket } from '../hooks/useWebSocket'
import './IDE.css'

// Modelica语言定义
const modelicaLanguageConfig: monaco.languages.LanguageConfiguration = {
  comments: { lineComment: '//', blockComment: ['/*', '*/'] },
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
  ]
}

const modelicaTokenProvider: monaco.languages.IMonarchLanguage = {
  defaultToken: '',
  tokenPostfix: '.modelica',
  keywords: [
    'algorithm', 'and', 'annotation', 'block', 'break', 'class', 'connect', 'connector',
    'constant', 'der', 'discrete', 'each', 'else', 'elseif', 'elsewhen', 'encapsulated',
    'end', 'enumeration', 'equation', 'extends', 'external', 'false', 'final', 'flow',
    'for', 'function', 'if', 'import', 'in', 'inner', 'input', 'loop', 'model', 'not',
    'or', 'outer', 'output', 'package', 'parameter', 'partial', 'protected', 'public',
    'record', 'redeclare', 'replaceable', 'return', 'stream', 'then', 'true', 'type',
    'when', 'while', 'within'
  ],
  typeKeywords: ['Real', 'Integer', 'Boolean', 'String'],
  operators: ['+', '-', '*', '/', '^', '=', ':=', '<>', '<', '<=', '>', '>=', 'and', 'or', 'not'],
  symbols: /[=><!~?:&|+\-*\/\^%]+/,
  tokenizer: {
    root: [
      [/[a-zA-Z_]\w*/, { cases: { '@keywords': 'keyword', '@typeKeywords': 'type', '@default': 'identifier' } }],
      { include: '@whitespace' },
      [/[{}()\[\]]/, '@brackets'],
      [/[;,]/, 'delimiter'],
      [/\d*\.\d+([eE][\-+]?\d+)?/, 'number.float'],
      [/\d+/, 'number'],
      [/"([^"\\]|\\.)*$/, 'string.invalid'],
      [/"/, { token: 'string.quote', bracket: '@open', next: '@string' }],
      [/@symbols/, { cases: { '@operators': 'operator', '@default': '' } }]
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

export default function IDE() {
  const { projectId } = useParams<{ projectId: string }>()
  const editorRef = useRef<HTMLDivElement>(null)
  const editorInstance = useRef<monaco.editor.IStandaloneCodeEditor | null>(null)
  const [output, setOutput] = useState<string[]>([])
  const [errors, setErrors] = useState<any[]>([])

  const { currentFile, updateFileContent, compile } = useProjectStore()
  const { connected, users, sendMessage } = useWebSocket(projectId || '')

  // 初始化Monaco编辑器
  useEffect(() => {
    if (!editorRef.current) return

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
        { token: 'comment', foreground: '6A9955' }
      ],
      colors: { 'editor.background': '#1E1E1E' }
    })

    // 创建编辑器
    editorInstance.current = monaco.editor.create(editorRef.current, {
      value: currentFile?.content || '// Start coding in Modelica',
      language: 'modelica',
      theme: 'modelica-dark',
      fontSize: 14,
      fontFamily: 'Consolas, Monaco, monospace',
      minimap: { enabled: true },
      scrollBeyondLastLine: false,
      automaticLayout: true
    })

    // 监听内容变化
    editorInstance.current.onDidChangeModelContent(() => {
      const content = editorInstance.current?.getValue() || ''
      updateFileContent(content)
      // 广播更新
      sendMessage({
        type: 'code_update',
        payload: { content, fileId: currentFile?.id }
      })
    })

    return () => {
      editorInstance.current?.dispose()
    }
  }, [])

  // 更新编辑器内容
  useEffect(() => {
    if (editorInstance.current && currentFile) {
      const currentValue = editorInstance.current.getValue()
      const content = currentFile.content || ''
      if (currentValue !== content) {
        editorInstance.current.setValue(content)
      }
    }
  }, [currentFile])

  // 编译代码
  const handleCompile = async () => {
    if (!currentFile) return

    setOutput(['Compiling...'])
    setErrors([])

    try {
      const result = await compile()
      if (result.success) {
        setOutput(prev => [...prev, 'Compilation successful!'])
      } else {
        setErrors(result.errors)
        setOutput(prev => [...prev, `Compilation failed: ${result.errors.length} errors`])
      }
    } catch (error) {
      setOutput(prev => [...prev, `Error: ${error}`])
    }
  }

  return (
    <div className="ide-container">
      {/* 工具栏 */}
      <header className="toolbar">
        <div className="toolbar-left">
          <button className="btn primary" onClick={handleCompile}>
            ▶ Compile
          </button>
          <span className="status">
            {connected ? '🟢 Connected' : '🔴 Disconnected'}
          </span>
        </div>
        <div className="toolbar-right">
          <span className="users">
            {users.length > 0 && `${users.length} user(s) online`}
          </span>
        </div>
      </header>

      {/* 主内容 */}
      <main className="main-content">
        {/* 编辑器 */}
        <div className="editor-panel">
          <div ref={editorRef} className="editor" />
        </div>

        {/* 右侧面板 */}
        <aside className="side-panel">
          <div className="panel-tabs">
            <button className="active">Output</button>
            <button>Errors ({errors.length})</button>
          </div>
          <div className="panel-content">
            <div className="output">
              {output.map((line, i) => (
                <div key={i} className="output-line">{line}</div>
              ))}
            </div>
            {errors.length > 0 && (
              <div className="errors">
                {errors.map((error, i) => (
                  <div key={i} className="error-item">
                    <span className="error-icon">⚠</span>
                    <span>{error.message}</span>
                    {error.line && (
                      <span className="error-location">
                        Line {error.line}, Col {error.column}
                      </span>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </aside>
      </main>

      {/* 状态栏 */}
      <footer className="status-bar">
        <span>{currentFile?.name || 'No file'}</span>
        <span>Modelica IDE Online</span>
      </footer>
    </div>
  )
}