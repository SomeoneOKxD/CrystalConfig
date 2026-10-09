plugins {
    id("net.fabricmc.fabric-loom")
}

val controller = project(":config-tester")
val mc = sc.current.version
val fabricLoader = property("loader_version") as String
val fabricApi = property("fabric_api_version") as String
val testerVersion = "0.1.0"

group = "dev.someoneok"
version = testerVersion
base {
    archivesName.set("crystalconfig-tester")
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    implementation("net.fabricmc:fabric-loader:$fabricLoader")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApi")

    implementation(project(":crystal-config:$mc"))
    implementation(project(":core"))
    implementation(project(":render-api"))
    implementation(project(":bridge-minecraft"))
}

loom {
    enableTransitiveAccessWideners.set(false)

    fabricModJsonPath = controller.file("src/main/resources/fabric.mod.json")
    runConfigs.named("client") {
        isIdeConfigGenerated = false
    }
    runConfigs.named("server") {
        isIdeConfigGenerated = false
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks {
    compileJava {
        options.release.set(25)
        options.encoding = "UTF-8"
    }
    processResources {
        inputs.property("minecraft_version", mc)
        inputs.property("loader_version", fabricLoader)
        inputs.property("fabric_api_version", fabricApi)
        filesMatching("fabric.mod.json") {
            expand(mapOf(
                "version" to testerVersion,
                "minecraft_version" to mc,
                "loader_version" to fabricLoader
            ))
        }
    }
    jar {
        archiveBaseName.set("crystalconfig-tester")
        archiveVersion.set("$testerVersion-mc$mc")
        archiveClassifier.set("")
    }
    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the config tester for Minecraft $mc and collects its jar."
        dependsOn(named("jar"))
        from(named<Jar>("jar").flatMap { it.archiveFile })
        into(controller.layout.buildDirectory.dir("libs"))
    }
}
