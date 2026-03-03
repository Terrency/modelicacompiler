package org.modelica.ide.services

import org.modelica.compiler.CompilationOptions
import org.modelica.compiler.ModelicaCompiler
import org.modelica.ide.models.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 编译服务
 */
class CompilerService {
    private val compiler = ModelicaCompiler(
        CompilationOptions(verbose = false)
    )

    /**
     * 编译代码
     */
    fun compile(code: String, fileName: String? = null): CompileResult {
        return try {
            val result = compiler.compile(code, fileName)

            val errors = result.lexerErrors.map { parseError(it) } +
                    result.parserErrors.map { parseError(it) } +
                    result.semanticErrors.map { parseError(it) }

            CompileResult(
                success = result.success,
                errors = errors,
                output = result.codegenErrors,
                generatedFiles = result.outputClasses.keys.toList()
            )
        } catch (e: Exception) {
            CompileResult(
                success = false,
                errors = listOf(CompileError("Compilation failed: ${e.message}"))
            )
        }
    }

    private fun parseError(errorStr: String): CompileError {
        // 简单解析错误字符串，提取行号和列号
        val lineMatch = Regex("at (\\d+):(\\d+)").find(errorStr)
        return CompileError(
            message = errorStr,
            line = lineMatch?.groupValues?.get(1)?.toIntOrNull(),
            column = lineMatch?.groupValues?.get(2)?.toIntOrNull()
        )
    }
}

/**
 * 项目服务
 */
class ProjectService {
    private val projects = ConcurrentHashMap<String, Project>()
    private val projectCounter = java.util.concurrent.atomic.AtomicLong(0)

    init {
        // 创建默认项目
        val defaultProject = Project(
            id = "default",
            name = "Default Project",
            description = "Default Modelica project",
            files = listOf(
                ProjectFile(
                    id = "main",
                    name = "Main.mo",
                    path = "Main.mo",
                    content = """
                        model Main
                          Real x(start = 1);
                          Real y(start = 2);
                        equation
                          der(x) = -y;
                          der(y) = x;
                        end Main;
                    """.trimIndent()
                )
            )
        )
        projects["default"] = defaultProject
    }

    /**
     * 获取所有项目
     */
    fun getAllProjects(): List<Project> = projects.values.toList()

    /**
     * 获取项目
     */
    fun getProject(id: String): Project? = projects[id]

    /**
     * 创建项目
     */
    fun createProject(request: CreateProjectRequest): Project {
        val id = "proj_${projectCounter.incrementAndGet()}"
        val project = Project(
            id = id,
            name = request.name,
            description = request.description,
            files = listOf(
                ProjectFile(
                    id = "${id}_main",
                    name = "Main.mo",
                    path = "Main.mo",
                    content = """
                        model Main
                          // Your model here
                        equation
                          // Your equations here
                        end Main;
                    """.trimIndent()
                )
            )
        )
        projects[id] = project
        return project
    }

    /**
     * 更新项目
     */
    fun updateProject(id: String, request: UpdateProjectRequest): Project? {
        val existing = projects[id] ?: return null
        val updated = existing.copy(
            name = request.name ?: existing.name,
            description = request.description ?: existing.description,
            updatedAt = System.currentTimeMillis()
        )
        projects[id] = updated
        return updated
    }

    /**
     * 删除项目
     */
    fun deleteProject(id: String): Boolean {
        return projects.remove(id) != null
    }
}

/**
 * 文件服务
 */
class FileService(private val projectService: ProjectService) {
    private val fileCounter = java.util.concurrent.atomic.AtomicLong(0)

    /**
     * 获取文件
     */
    fun getFile(projectId: String, fileId: String): ProjectFile? {
        val project = projectService.getProject(projectId) ?: return null
        return project.files.find { it.id == fileId }
    }

    /**
     * 创建文件
     */
    fun createFile(projectId: String, request: CreateFileRequest): ProjectFile? {
        val project = projectService.getProject(projectId) ?: return null
        val fileId = "file_${fileCounter.incrementAndGet()}"
        val file = ProjectFile(
            id = fileId,
            name = request.name,
            path = request.path ?: request.name,
            content = request.content
        )

        val updatedProject = project.copy(
            files = project.files + file,
            updatedAt = System.currentTimeMillis()
        )
        projectService.updateProject(projectId, UpdateProjectRequest())

        return file
    }

    /**
     * 更新文件内容
     */
    fun updateFile(projectId: String, fileId: String, request: UpdateFileRequest): ProjectFile? {
        val project = projectService.getProject(projectId) ?: return null
        val fileIndex = project.files.indexOfFirst { it.id == fileId }
        if (fileIndex < 0) return null

        val updatedFile = project.files[fileIndex].copy(
            content = request.content,
            lastModified = System.currentTimeMillis()
        )

        val updatedFiles = project.files.toMutableList()
        updatedFiles[fileIndex] = updatedFile

        return updatedFile
    }

    /**
     * 删除文件
     */
    fun deleteFile(projectId: String, fileId: String): Boolean {
        val project = projectService.getProject(projectId) ?: return false
        val fileIndex = project.files.indexOfFirst { it.id == fileId }
        if (fileIndex < 0) return false

        val updatedFiles = project.files.toMutableList()
        updatedFiles.removeAt(fileIndex)

        return true
    }
}