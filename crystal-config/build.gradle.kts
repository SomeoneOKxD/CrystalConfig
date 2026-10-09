import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("net.fabricmc.fabric-loom")
    id("com.gradleup.shadow")
    `maven-publish`
}

val controller = project(":crystal-config")
val modVersion = property("mod_version") as String
val minecraftVersion = sc.current.version
val loaderVersion = property("loader_version") as String
val baseName = property("archives_base_name") as String
val jarVersion = "$modVersion-mc$minecraftVersion"

val isJitPack = System.getenv("JITPACK").equals("true", ignoreCase = true)
val jitPackVersion = System.getenv("VERSION")
val officialJitPackGroup = "com.github.SomeoneOKxD"
val officialJitPackArtifact = "CrystalConfig"
val versionedArtifactId = if (isJitPack) "$officialJitPackArtifact-$minecraftVersion" else "$baseName-$minecraftVersion"

group = if (isJitPack) officialJitPackGroup else property("maven_group") as String
version = if (isJitPack && !jitPackVersion.isNullOrBlank()) jitPackVersion else modVersion

val bridge = project(":bridge-minecraft")
val core = project(":core")
val renderApi = project(":render-api")

val shadowImpl = configurations.create("shadowImpl") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

configurations.implementation.get().extendsFrom(shadowImpl)

val controllerDirectory = controller.layout.projectDirectory
val sharedResourcesDirectory = controllerDirectory.dir("src/main/resources")
val compatibilityResourcesDirectory = controllerDirectory.dir("src/compat/$minecraftVersion/resources")
val accessWidenerFile = controllerDirectory.file(
    when (minecraftVersion) {
        "26.1" -> "src/compat/26.1/crystalconfig.accesswidener"
        "26.2" -> "src/compat/26.2/crystalconfig.accesswidener"
        "26.3" -> "src/compat/26.3/crystalconfig.accesswidener"
        else -> error("Unsupported Minecraft version: $minecraftVersion")
    }
)

val msdfAtlasGenExecutable = rootProject.layout.projectDirectory.file("tools/msdf-atlas-gen/msdf-atlas-gen.exe")
val msdfTextCharsetFile = sharedResourcesDirectory.file("assets/crystalconfig/fonts/charset/text.charset")
val msdfSymbolsCharsetFile = sharedResourcesDirectory.file("assets/crystalconfig/fonts/charset/symbols.charset")
val msdfMediaBrandsCharsetFile = sharedResourcesDirectory.file("assets/crystalconfig/fonts/charset/media-brands.charset")

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
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")

    shadowImpl(project(":core"))
    shadowImpl(project(":bridge-minecraft"))
    shadowImpl(project(":render-api"))
}

loom {
    fabricModJsonPath = controller.file("src/main/resources/fabric.mod.json")
    accessWidenerPath = accessWidenerFile.asFile

    runConfigs.named("client") { isIdeConfigGenerated = false }
    runConfigs.named("server") { isIdeConfigGenerated = false }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks {
    val generateMsdfFontTasks = msdfFaces.map { spec ->
        val sourceFont = sharedResourcesDirectory.file("assets/crystalconfig/fonts/source/${spec.sourceFileName}")
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
            outputs.file(sharedResourcesDirectory.file("assets/crystalconfig/textures/msdf/$face.png"))
            outputs.file(sharedResourcesDirectory.file("assets/crystalconfig/msdf/$face.json"))

            doFirst {
                val fontFile = sourceFont.asFile
                if (!fontFile.exists()) {
                    throw GradleException(
                        "Missing source TTF for '$face': ${fontFile.absolutePath}. " +
                            "Add src/main/resources/assets/crystalconfig/fonts/source/${spec.sourceFileName}"
                    )
                }
                sharedResourcesDirectory.dir("assets/crystalconfig/textures/msdf").asFile.mkdirs()
                sharedResourcesDirectory.dir("assets/crystalconfig/msdf").asFile.mkdirs()
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
                    "-json", sharedResourcesDirectory.file("assets/crystalconfig/msdf/$face.json").asFile.absolutePath,
                    "-imageout", sharedResourcesDirectory.file("assets/crystalconfig/textures/msdf/$face.png").asFile.absolutePath,
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
                sharedResourcesDirectory.file("assets/crystalconfig/textures/msdf/fallback-latin.png"),
                sharedResourcesDirectory.file("assets/crystalconfig/msdf/fallback-latin.json")
            )
        }
    }

    processResources {
        exclude("assets/crystalconfig/fonts/**")

        inputs.property("mod_version", modVersion)
        inputs.property("loader_version", loaderVersion)
        inputs.property("minecraft_version", minecraftVersion)

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to modVersion,
                    "loader_version" to loaderVersion,
                    "minecraft_version" to minecraftVersion,
                )
            )
        }

        from(compatibilityResourcesDirectory)
        from(accessWidenerFile) {
            rename { "crystalconfig.accesswidener" }
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
        from(compatibilityResourcesDirectory)
        from(accessWidenerFile) {
            rename { "crystalconfig.accesswidener" }
        }

        from(core.extensions.getByType<SourceSetContainer>()["main"].allSource)
        from(bridge.extensions.getByType<SourceSetContainer>()["main"].allSource)
        from(renderApi.extensions.getByType<SourceSetContainer>()["main"].allSource)
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds Minecraft $minecraftVersion and copies its release and sources jars to crystal-config/build/libs."
        dependsOn(named("shadowJar"), named("sourcesJar"))
        from(named<ShadowJar>("shadowJar").flatMap { it.archiveFile })
        from(named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(controller.layout.buildDirectory.dir("libs"))
    }
}

fun MavenPublication.configureCrystalConfigPublication(artifact: String, displayName: String, publicationGroup: String) {
    groupId = publicationGroup
    artifactId = artifact
    version = project.version.toString()

    artifact(tasks.named("shadowJar"))
    artifact(tasks.named("sourcesJar"))

    pom {
        name.set(displayName)
        description.set("A modern Fabric configuration UI library for Minecraft $minecraftVersion, published as the shaded Fabric mod jar.")
    }
}

publishing {
    publications {
        create<MavenPublication>("versioned") {
            configureCrystalConfigPublication(
                versionedArtifactId,
                "CrystalConfig for Minecraft $minecraftVersion",
                if (isJitPack) "$officialJitPackGroup.$officialJitPackArtifact" else project.group.toString()
            )
        }

        if (minecraftVersion == "26.1") {
            create<MavenPublication>("baseAlias") {
                val artifact = if (isJitPack) officialJitPackArtifact else baseName
                configureCrystalConfigPublication(artifact, "CrystalConfig for Minecraft 26.1", project.group.toString())
            }
        }
    }
}
