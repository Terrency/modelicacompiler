import { useEffect, useRef, useState } from 'react'

interface WebSocketMessage {
  type: string
  payload?: any
}

interface User {
  userId: string
  userName: string
}

export function useWebSocket(projectId: string) {
  const [connected, setConnected] = useState(false)
  const [users, setUsers] = useState<User[]>([])
  const socketRef = useRef<WebSocket | null>(null)

  useEffect(() => {
    if (!projectId) return

    const wsUrl = `${import.meta.env.VITE_WS_URL || 'ws://localhost:8080'}/ws/editor/${projectId}`
    const socket = new WebSocket(wsUrl)
    socketRef.current = socket

    socket.onopen = () => {
      setConnected(true)
      console.log('WebSocket connected')
    }

    socket.onclose = () => {
      setConnected(false)
      console.log('WebSocket disconnected')
    }

    socket.onerror = (error) => {
      console.error('WebSocket error:', error)
    }

    socket.onmessage = (event) => {
      try {
        const message: WebSocketMessage = JSON.parse(event.data)
        handleMessage(message)
      } catch (error) {
        console.error('Failed to parse message:', error)
      }
    }

    return () => {
      socket.close()
    }
  }, [projectId])

  const handleMessage = (message: WebSocketMessage) => {
    switch (message.type) {
      case 'connected':
        setUsers(message.payload.users || [])
        break
      case 'user_joined':
        setUsers(prev => [...prev, {
          userId: message.payload.userId,
          userName: message.payload.userName
        }])
        break
      case 'user_left':
        setUsers(prev => prev.filter(u => u.userId !== message.payload.userId))
        break
      case 'code_update':
        // 由编辑器组件处理
        break
      case 'cursor_update':
        // 可以显示其他用户的光标
        break
    }
  }

  const sendMessage = (message: WebSocketMessage) => {
    if (socketRef.current?.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify(message))
    }
  }

  return {
    connected,
    users,
    sendMessage
  }
}