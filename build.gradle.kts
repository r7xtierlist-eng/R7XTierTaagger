plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.14-SNAPSHOT"
    id("io.freefair.lombok") version "9.1.0"
}

val modVersion = project.property("mod_version") as String
val universalBuild = project.findProperty("universal_build")?.toString()?.toBoolean() == true
version = if (universalBuild) "$modVersion+universal" else "$modVersion+mc${project.property("minecraft_version")}"
group = project.property("maven_group") as String

val minecraftVersion = project.property("minecraft_version") as String
val ukulibDependencyVersion = project.property("ukulib_dependency_version") as String
val minecraftDependency = if (universalBuild) ">=1.21.5 <=1.21.11" else minecraftVersion
val ukulibDependency = if (universalBuild) ">=1.7.1 <2.0.0" else "^$ukulibDependencyVersion"

sourceSets {
    main {
        java.srcDir(
            if (minecraftVersion == "1.21" || minecraftVersion == "1.21.1") {
                "src/compat/legacy-display/java"
            } else {
                "src/compat/render-state/java"
            }
        )
    }
}

repositories {
    maven {
        url = uri("https://maven.uku3lig.net/releases")
    }
    maven {
        url = uri("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    modImplementation(fabricApi.module("fabric-command-api-v2", project.property("fabric_api_version") as String))

    modApi("net.uku3lig:ukulib:${project.property("ukulib_version")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${project.property("devauth_version")}")
}

base {
    archivesName = project.property("archives_base_name") as String
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_dependency", minecraftDependency)
    inputs.property("ukulib_dependency", ukulibDependency)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_dependency" to minecraftDependency,
            "ukulib_dependency" to ukulibDependency
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}
