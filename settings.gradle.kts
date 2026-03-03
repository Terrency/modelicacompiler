rootProject.name = "modelicacompiler"

// 模块一：编译器核心
include("modelica-compiler")

// 模块二：桌面IDE的Kotlin桥接模块
include("modelica-ide-desktop:native")

// 模块三：在线IDE后端
include("modelica-ide-online:backend")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}