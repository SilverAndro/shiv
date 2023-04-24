plugins {
    id("java")
}

group = "io.github.silverandro.shiv"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("asm:asm-tree:3.3.1")
}

tasks {
    jar {
        manifest.attributes(
            "Agent-Class" to "io.github.silverandro.shiv.mayo.Mayo",
            "Can-Redefine-Classes" to true,
            "Can-Retransform-Classes" to true
        )
    }
}
