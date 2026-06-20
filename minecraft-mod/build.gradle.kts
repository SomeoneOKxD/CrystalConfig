import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("net.fabricmc.fabric-loom")
    id("com.gradleup.shadow")
    `maven-publish`
}

val modVersion = property("mod_version") as String
val minecraftVersion = property("minecraft_version") as String
val baseName = property("archives_base_name") as String
val jarVersion = "$modVersion+mc$minecraftVersion"

val isJitPack = System.getenv("JITPACK").equals("true", ignoreCase = true)
val jitPackVersion = System.getenv("VERSION")
val officialJitPackGroup = "com.github.SomeoneOKxD.CrystalConfig"

group = if (isJitPack) officialJitPackGroup else property("maven_group") as String
version = if (isJitPack && !jitPackVersion.isNullOrBlank()) jitPackVersion else modVersion

val bridge = project(":bridge-minecraft")
val core = project(":core")

val shadowImpl by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

configurations.implementation.get().extendsFrom(shadowImpl)

val msdfAtlasGenExecutable = rootProject.layout.projectDirectory.file("tools/msdf-atlas-gen/msdf-atlas-gen.exe")

val msdfTextCharsetFile = layout.projectDirectory.file("src/main/resources/assets/crystalconfig/fonts/charset/text.charset")
val msdfSymbolsCharsetFile = layout.projectDirectory.file("src/main/resources/assets/crystalconfig/fonts/charset/symbols.charset")
val msdfMediaBrandsCharsetFile = layout.projectDirectory.file("src/main/resources/assets/crystalconfig/fonts/charset/media-brands.charset")

data class MsdfFaceSpec(
    val name: String,
    val sourceFileName: String,
    val charsetFile: RegularFile,
)

val msdfFaces = listOf(
    MsdfFaceSpec("regular", "regular.ttf", msdfTextCharsetFile),
    MsdfFaceSpec("medium", "medium.ttf", msdfTextCharsetFile),
    MsdfFaceSpec("semibold", "semibold.ttf", msdfTextCharsetFile),
    MsdfFaceSpec("fallback-symbols", "fallback-symbols.ttf", msdfSymbolsCharsetFile),
    MsdfFaceSpec("media-brands", "media-brands.ttf", msdfMediaBrandsCharsetFile)
)

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    shadowImpl(core)
    shadowImpl(bridge)
}

loom {
    runConfigs.named("client") { isIdeConfigGenerated = false }
    runConfigs.named("server") { isIdeConfigGenerated = false }

    accessWidenerPath = file("src/main/resources/crystalconfig.accesswidener")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }

    withSourcesJar()
    withJavadocJar()
}

tasks {
    val generateMsdfFontTasks = msdfFaces.map { spec ->
        val sourceFont = layout.projectDirectory.file("src/main/resources/assets/crystalconfig/fonts/source/${spec.sourceFileName}")
        val charsetFile = spec.charsetFile
        val face = spec.name
        val taskSuffix = face.split("-").joinToString("") { part ->
            part.replaceFirstChar { char -> char.uppercaseChar() }
        }

        register("generateMsdf${taskSuffix}Font", Exec::class) {
            group = "crystalconfig"
            description = "Generates the $face MSDF PNG atlas and JSON metrics from a TTF font."
            inputs.file(sourceFont)
            inputs.file(charsetFile)
            outputs.file(layout.projectDirectory.file("src/main/resources/assets/crystalconfig/textures/msdf/$face.png"))
            outputs.file(layout.projectDirectory.file("src/main/resources/assets/crystalconfig/msdf/$face.json"))

            doFirst {
                val fontFile = sourceFont.asFile
                if (!fontFile.exists()) {
                    throw GradleException(
                        "Missing source TTF for '$face': ${fontFile.absolutePath}. " +
                            "Add src/main/resources/assets/crystalconfig/fonts/source/${spec.sourceFileName}"
                    )
                }
                layout.projectDirectory.dir("src/main/resources/assets/crystalconfig/textures/msdf").asFile.mkdirs()
                layout.projectDirectory.dir("src/main/resources/assets/crystalconfig/msdf").asFile.mkdirs()
            }

            doFirst {
                val exe = msdfAtlasGenExecutable.asFile
                if (!exe.exists()) {
                    throw GradleException(
                        "Missing local msdf-atlas-gen executable: ${exe.absolutePath}. " +
                            "Put msdf-atlas-gen.exe in tools/msdf-atlas-gen/ at the project root."
                    )
                }
                logger.lifecycle("Using local msdf-atlas-gen executable: ${exe.absolutePath}")
                commandLine(
                    exe.absolutePath,
                    "-font", sourceFont.asFile.absolutePath,
                    "-charset", charsetFile.asFile.absolutePath,
                    "-type", "msdf",
                    "-format", "png",
                    "-json", layout.projectDirectory.file("src/main/resources/assets/crystalconfig/msdf/$face.json").asFile.absolutePath,
                    "-imageout", layout.projectDirectory.file("src/main/resources/assets/crystalconfig/textures/msdf/$face.png").asFile.absolutePath,
                    "-size", "24",
                    "-pxrange", "2",
                    "-potr"
                )
            }
        }
    }

    register("generateMsdfFonts") {
        group = "crystalconfig"
        description = "Generates all CrystalConfig MSDF font atlases from source TTF files."
        dependsOn(generateMsdfFontTasks)
        doFirst {
            delete(
                layout.projectDirectory.file("src/main/resources/assets/crystalconfig/textures/msdf/fallback-latin.png"),
                layout.projectDirectory.file("src/main/resources/assets/crystalconfig/msdf/fallback-latin.json")
            )
        }
    }

    processResources {
        exclude("assets/crystalconfig/fonts/**")

        filesMatching("fabric.mod.json") {
            expand(project.properties)
        }
    }

    compileJava {
        sourceCompatibility = "25"
        targetCompatibility = "25"
        options.release.set(25)
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }

    withType<Jar>().configureEach {
        archiveBaseName.set(baseName)
        archiveVersion.set(jarVersion)
    }

    named<Jar>("jar") {
        archiveClassifier.set("dev")
    }

    named<ShadowJar>("shadowJar") {
        configurations = listOf(shadowImpl)
        archiveBaseName.set(baseName)
        archiveVersion.set(jarVersion)
        archiveClassifier.set("")
        mergeServiceFiles()
    }

    assemble {
        dependsOn(named("shadowJar"), named("sourcesJar"))
    }

    build {
        dependsOn(named("shadowJar"), named("sourcesJar"))
    }

    named<Jar>("sourcesJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(sourceSets.main.get().allSource)

        from(core.extensions.getByType<SourceSetContainer>()["main"].allSource)
        from(bridge.extensions.getByType<SourceSetContainer>()["main"].allSource)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = baseName
            version = project.version.toString()

            artifact(tasks.named("shadowJar"))
            artifact(tasks.named("sourcesJar"))

            pom {
                name.set("CrystalConfig")
                description.set("A modern Fabric configuration UI library for Minecraft mods, published as the shaded Fabric mod jar.")
            }
        }
    }
}
