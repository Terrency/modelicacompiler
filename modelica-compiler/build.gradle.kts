plugins {
    kotlin("jvm")
}

dependencies {
    // ASM字节码操作
    implementation(libs.asm.core)
    implementation(libs.asm.commons)
    implementation(libs.asm.tree)

    // 测试
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.modelica.compiler.ModelicaCompiler"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}