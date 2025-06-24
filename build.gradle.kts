import dev.dediamondpro.buildsource.Platform
import dev.dediamondpro.buildsource.VersionDefinition
import dev.dediamondpro.buildsource.VersionRange

plugins {
    alias(libs.plugins.arch.loom)
    // Removed publishing plugin
}

buildscript {
    extra["loom.platform"] = project.name.split('-')[1]
}

val mcPlatform = Platform.fromProject(project)
val mod_name: String by project
val mod_version: String by project
val mod_id: String by project

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net")
    maven("https://maven.parchmentmc.org")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.minecraftforge.net")
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.dediamondpro.dev/releases")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
}

stonecutter {
    const("fabric", mcPlatform.isFabric)
    const("forge", mcPlatform.isForge)
    const("neoforge", mcPlatform.isNeoForge)
    const("forgelike", mcPlatform.isForgeLike)

    swap("mod_name", "\"$mod_name\"")
    swap("mod_id", "\"$mod_id\"")
    swap("mod_version", "\"$mod_version\"")
}

val mcVersion = VersionDefinition(
    "1.21.6" to VersionRange("1.21.6", "1.21.6", name = "1.21.6"),
)
val parchmentVersion = VersionDefinition(
    "1.21.5" to "1.21.5:2025.06.15",
    "1.21.6" to "1.21.5:2025.06.15", // reuse latest mapping
)
val fabricApiVersion = VersionDefinition(
    "1.21.6" to "0.128.0+1.21.6",
)
val modMenuVersion = VersionDefinition(
    "1.21.6" to "15.0.0-beta.3",
)
val neoForgeVersion = VersionDefinition(
    "1.21.6" to "21.6.15-beta",
)
val yaclVersion = VersionDefinition(
    "1.21.6-fabric" to "3.7.1+1.21.6-fabric",
    "1.21.6-neoforge" to "3.7.1+1.21.6-neoforge",
)

// As of now this mod is not available on 1.21.6
//val noChatReportsVersion = VersionDefinition(
//    "1.21.4-fabric" to "Fabric-1.21.4-v2.11.0",
//    "1.21.4-neoforge" to "NeoForge-1.21.4-v2.11.0",
//)

dependencies {
    minecraft("com.mojang:minecraft:${mcPlatform.versionString}")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchmentVersion.getOrNull(mcPlatform)?.let {
            parchment("org.parchmentmc.data:parchment-$it@zip")
        }
    })

    if (mcPlatform.isFabric) {
        modImplementation("net.fabricmc:fabric-loader:0.16.14")
        modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion.get(mcPlatform)}")
        modImplementation("com.terraformersmc:modmenu:${modMenuVersion.get(mcPlatform)}")
    } else if (mcPlatform.isNeoForge) {
        "neoForge"("net.neoforged:neoforge:${neoForgeVersion.get(mcPlatform)}")
    }

    modImplementation("dev.isxander:yet-another-config-lib:${yaclVersion.get(mcPlatform)}")
    compileOnly(libs.objc)

//    noChatReportsVersion.getOrNull(mcPlatform)?.let {
//        modCompileOnly("maven.modrinth:no-chat-reports:$it")
//    }
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources/chatshot.accesswidener")
    if (mcPlatform.isForge) forge {
        convertAccessWideners.set(true)
        mixinConfig("mixins.resourcify.json")
    }
    runConfigs["client"].isIdeConfigGenerated = true
}

base.archivesName.set(
    "$mod_name (${
        mcVersion.get(mcPlatform).getName().replace("/", "-")
    }-${mcPlatform.loaderString})-$mod_version"
)

tasks {
    register<Copy>("copyJar") {
        File("${project.rootDir}/jars").mkdirs()
        from(remapJar.get().archiveFile)
        into("${project.rootDir}/jars")
    }
    clean { delete("${project.rootDir}/jars") }
    processResources {
        val properties = mapOf(
            "id" to mod_id,
            "name" to mod_name,
            "version" to mod_version,
            "mcVersion" to mcVersion.get(mcPlatform).getLoaderRange(mcPlatform),
        )
        properties.forEach { (k, v) -> inputs.property(k, v) }
        filesMatching(listOf("mcmod.info", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "fabric.mod.json")) {
            expand(properties)
        }
        if (!mcPlatform.isFabric) exclude("fabric.mod.json")
        if (!mcPlatform.isForgeLike) exclude("pack.mcmeta")
        if (!mcPlatform.isNeoForge) exclude("META-INF/neoforge.mods.toml")
    }
    remapJar {
        finalizedBy("copyJar")
        if (mcPlatform.isNeoForge) {
            atAccessWideners.add("chatshot.accesswidener")
        }
    }
    withType<Jar> {
        from(rootProject.file("LICENSE"))
        from(rootProject.file("LICENSE.LESSER"))
    }
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
