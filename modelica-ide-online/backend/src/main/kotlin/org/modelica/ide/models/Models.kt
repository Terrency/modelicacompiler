package org.modelica.ide.models

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 项目模型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Project(
    val id: String,
    val name: String,
    val description: String? = null,
    val files: List<ProjectFile> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 项目文件
 */
data class ProjectFile(
    val id: String,
    val name: String,
    val path: String,
    val content: String? = null,
    val size: Long = 0,
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * 编译请求
 */
data class CompileRequest(
    val code: String,
    val fileName: String? = null
)

/**
 * 编译结果
 */
data class CompileResult(
    val success: Boolean,
    val errors: List<CompileError> = emptyList(),
    val warnings: List<CompileError> = emptyList(),
    val output: List<String> = emptyList(),
    val generatedFiles: List<String> = emptyList()
)

/**
 * 编译错误
 */
data class CompileError(
    val message: String,
    val line: Int? = null,
    val column: Int? = null,
    val severity: String = "error"
)

/**
 * 创建项目请求
 */
data class CreateProjectRequest(
    val name: String,
    val description: String? = null
)

/**
 * 更新项目请求
 */
data class UpdateProjectRequest(
    val name: String? = null,
    val description: String? = null
)

/**
 * 创建文件请求
 */
data class CreateFileRequest(
    val name: String,
    val path: String? = null,
    val content: String = ""
)

/**
 * 更新文件请求
 */
data class UpdateFileRequest(
    val content: String
)

/**
 * WebSocket消息
 */
sealed class WsMessage {
    abstract val type: String

    data class CodeUpdate(
        override val type: String = "code_update",
        val fileId: String,
        val content: String,
        val version: Int
    ) : WsMessage()

    data class CursorMove(
        override val type: String = "cursor_move",
        val fileId: String,
        val userId: String,
        val line: Int,
        val column: Int
    ) : WsMessage()

    data class SelectionChange(
        override val type: String = "selection_change",
        val fileId: String,
        val userId: String,
        val startLine: Int,
        val startColumn: Int,
        val endLine: Int,
        val endColumn: Int
    ) : WsMessage()

    data class UserJoin(
        override val type: String = "user_join",
        val userId: String,
        val userName: String
    ) : WsMessage()

    data class UserLeave(
        override val type: String = "user_leave",
        val userId: String
    ) : WsMessage()
}