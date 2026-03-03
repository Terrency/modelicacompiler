package org.modelica.ide.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.modelica.ide.models.*
import org.modelica.ide.services.CompilerService

private val compilerService = CompilerService()

fun Route.compileRoutes() {
    route("/api/compile") {
        post {
            val request = call.receive<CompileRequest>()
            val result = compilerService.compile(request.code, request.fileName)
            call.respond(result)
        }
    }
}

fun Route.projectRoutes() {
    val projectService = org.modelica.ide.services.ProjectService()

    route("/api/projects") {
        get {
            call.respond(projectService.getAllProjects())
        }

        post {
            val request = call.receive<CreateProjectRequest>()
            val project = projectService.createProject(request)
            call.respond(HttpStatusCode.Created, project)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val project = projectService.getProject(id)
            if (project != null) {
                call.respond(project)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<UpdateProjectRequest>()
            val project = projectService.updateProject(id, request)
            if (project != null) {
                call.respond(project)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (projectService.deleteProject(id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

fun Route.fileRoutes() {
    val projectService = org.modelica.ide.services.ProjectService()
    val fileService = org.modelica.ide.services.FileService(projectService)

    route("/api/projects/{projectId}/files") {
        get("/{fileId}") {
            val projectId = call.parameters["projectId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val fileId = call.parameters["fileId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val file = fileService.getFile(projectId, fileId)
            if (file != null) {
                call.respond(file)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        post {
            val projectId = call.parameters["projectId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<CreateFileRequest>()
            val file = fileService.createFile(projectId, request)
            if (file != null) {
                call.respond(HttpStatusCode.Created, file)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Project not found"))
            }
        }

        put("/{fileId}") {
            val projectId = call.parameters["projectId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val fileId = call.parameters["fileId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<UpdateFileRequest>()
            val file = fileService.updateFile(projectId, fileId, request)
            if (file != null) {
                call.respond(file)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        delete("/{fileId}") {
            val projectId = call.parameters["projectId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val fileId = call.parameters["fileId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (fileService.deleteFile(projectId, fileId)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}