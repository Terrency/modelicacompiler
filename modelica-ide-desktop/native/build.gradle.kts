plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":modelica-compiler"))
    implementation(libs.asm.core)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.modelica.ide.CompilerBridge"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}