package org.modelica.ide.websocket

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * 编辑器WebSocket会话
 */
class EditorSession(
    val sessionId: String,
    val projectId: String,
    val userId: String,
    val userName: String,
    val socket: WebSocketSession
)

/**
 * 活跃会话管理
 */
object SessionManager {
    private val sessions = ConcurrentHashMap<String, MutableList<EditorSession>>()

    fun addSession(projectId: String, session: EditorSession) {
        sessions.computeIfAbsent(projectId) { Collections.synchronizedList(mutableListOf()) }
            .add(session)
    }

    fun removeSession(projectId: String, session: EditorSession) {
        sessions[projectId]?.remove(session)
    }

    fun getSessions(projectId: String): List<EditorSession> {
        return sessions[projectId]?.toList() ?: emptyList()
    }

    suspend fun broadcast(projectId: String, message: String, excludeSession: EditorSession? = null) {
        getSessions(projectId)
            .filter { it != excludeSession }
            .forEach { session ->
                try {
                    session.socket.send(message)
                } catch (e: Exception) {
                    // 忽略发送错误
                }
            }
    }
}

@Serializable
data class SocketMessage(
    val type: String,
    val payload: Map<String, String> = emptyMap()
)

fun Route.editorSocket() {
    val json = Json { ignoreUnknownKeys = true }

    webSocket("/ws/editor/{projectId}") {
        val projectId = call.parameters["projectId"] ?: return@webSocket close(
            CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing projectId")
        )

        // 生成用户ID和会话ID
        val sessionId = java.util.UUID.randomUUID().toString()
        val userId = "user_${System.currentTimeMillis()}"
        val userName = "User ${(0..9999).random()}"

        val session = EditorSession(sessionId, projectId, userId, userName, this)
        SessionManager.addSession(projectId, session)

        try {
            // 发送欢迎消息
            val welcomeMsg = json.encodeToString(
                SocketMessage("connected", mapOf(
                    "sessionId" to sessionId,
                    "userId" to userId,
                    "userName" to userName
                ))
            )
            send(welcomeMsg)

            // 广播用户加入
            val joinMsg = json.encodeToString(
                SocketMessage("user_joined", mapOf(
                    "userId" to userId,
                    "userName" to userName
                ))
            )
            SessionManager.broadcast(projectId, joinMsg, session)

            // 接收消息循环
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        try {
                            val message = json.decodeFromString<SocketMessage>(text)
                            handleMessage(projectId, session, message, json)
                        } catch (e: Exception) {
                            // 忽略解析错误
                        }
                    }
                    else -> {}
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            // 连接关闭
        } finally {
            SessionManager.removeSession(projectId, session)

            // 广播用户离开
            val leaveMsg = json.encodeToString(
                SocketMessage("user_left", mapOf("userId" to userId))
            )
            SessionManager.broadcast(projectId, leaveMsg, session)
        }
    }
}

private suspend fun handleMessage(
    projectId: String,
    session: EditorSession,
    message: SocketMessage,
    json: Json
) {
    when (message.type) {
        "code_update" -> {
            // 广播代码更新
            val broadcastMsg = json.encodeToString(
                SocketMessage("code_update", message.payload + ("userId" to session.userId))
            )
            SessionManager.broadcast(projectId, broadcastMsg, session)
        }
        "cursor_move" -> {
            // 广播光标移动
            val broadcastMsg = json.encodeToString(
                SocketMessage("cursor_update", message.payload + ("userId" to session.userId))
            )
            SessionManager.broadcast(projectId, broadcastMsg, session)
        }
        "selection_change" -> {
            // 广播选择变化
            val broadcastMsg = json.encodeToString(
                SocketMessage("selection_update", message.payload + ("userId" to session.userId))
            )
            SessionManager.broadcast(projectId, broadcastMsg, session)
        }
    }
}