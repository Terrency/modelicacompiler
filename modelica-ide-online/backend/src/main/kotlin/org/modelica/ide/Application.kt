package org.modelica.ide

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.serialization.jackson.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import org.modelica.ide.routes.compileRoutes
import org.modelica.ide.routes.projectRoutes
import org.modelica.ide.routes.fileRoutes
import org.modelica.ide.websocket.editorSocket
import java.time.Duration

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // 安装插件
    install(ContentNegotiation) {
        jackson()
    }

    install(CORS) {
        anyHost()
        allowHeader("*")
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(30)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    // 配置路由
    routing {
        compileRoutes()
        projectRoutes()
        fileRoutes()
        editorSocket()
    }
}