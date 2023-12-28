plugins {
    `maven-publish`
    kotlin("jvm") version "1.9.21"
    id("fabric-loom") version "1.4-SNAPSHOT"
}

val kotlin = property("deps.kotlin").toString()
val mcVersion = stonecutter.current.version
val isActive = stonecutter.current.isActive

val modId = property("mod.name").toString()
val modVersion = property("mod.version").toString()
val target = ">=${property("mod.min_target")}- <=${property("mod.max_target")}"

repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("net.fabricmc:yarn:${property("deps.yarn")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("deps.flk")}+kotlin.$kotlin")
    modImplementation(fabricApi.module("fabric-renderer-api-v1", "${property("deps.fabric_api")}"))
    modImplementation(fabricApi.module("fabric-renderer-indigo", "${property("deps.fabric_api")}"))
    modRuntimeOnly(fabricApi.module("fabric-rendering-fluids-v1", "${property("deps.fabric_api")}"))

    // Test
    modImplementation(fabricApi.module("fabric-command-api-v2", "${property("deps.fabric_api")}"))
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources/$modId.accesswidener")
}

tasks.processResources {
    filesMatching("fabric.mod.json") {
        expand(
            "mod_version" to modVersion,
            "target_minecraft" to target
        )
    }
}

sourceSets {
    test {
        compileClasspath += main.get().compileClasspath
        runtimeClasspath += main.get().runtimeClasspath
    }
}

if (stonecutter.current.isActive) loom {
    runConfigs["client"].ideConfigGenerated(true)
    runConfigs.all {
        runDir("../../run")
        vmArgs("-Dmixin.debug.export=true")
    }
    runs {
        create("testClient") {
            name = "Testmod Client"
            client()
            source(sourceSets["test"])
        }
    }
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

publishing {
    repositories {
        maven("https://maven.kikugie.dev/releases") {
            name = "kikugieMaven"
            credentials(PasswordCredentials::class.java)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.maven_group")}.$modId"
            artifactId = modVersion
            version = mcVersion

            from(components["java"])
        }
    }
}