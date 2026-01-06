import java.util.Base64

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.maven.central.publish)
}

android {
    namespace = "com.michaeltchuang.algokit"
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
    compileSdk = libs.versions.compileSdk.get().toInt()
}

// Configuration for AAR publications - just add more entries here!
data class AarConfig(
    val publicationName: String,
    val artifactId: String,
    val aarFileName: String,
    val displayName: String
)

val aarConfigs = listOf(
    AarConfig(
        publicationName = "AlgorandFoundationCrypto",
        artifactId = "algorand-foundation-crypto",
        aarFileName = "crypto-debug.aar",
        displayName = "AlgorandFoundationCrypto"
    ),
    AarConfig(
        publicationName = "AlgorandFoundationProvider",
        artifactId = "algorand-foundation-provider",
        aarFileName = "provider-debug.aar",
        displayName = "AlgorandFoundationProvider"
    )
)

// Create JAR tasks dynamically for each AAR configuration
val jarTasks = aarConfigs.associate { config ->
    val sourcesJar = tasks.register<Jar>("${config.publicationName}SourcesJar") {
        archiveClassifier.set("sources")
        archiveBaseName.set(config.artifactId)
        from(android.sourceSets.getByName("main").java.srcDirs)
    }

    val javadocJar = tasks.register<Jar>("${config.publicationName}JavadocJar") {
        archiveClassifier.set("javadoc")
        archiveBaseName.set(config.artifactId)
    }

    config.publicationName to (sourcesJar to javadocJar)
}

afterEvaluate {
    val versionTag = "0.1.0"
    val groupId = "com.michaeltchuang.algokit"

    publishing {
        publications {
            // Create publications dynamically from configuration
            aarConfigs.forEach { config ->
                create<MavenPublication>(config.publicationName) {
                    artifact(file(config.aarFileName)) {
                        extension = "aar"
                    }

                    val (sourcesJar, javadocJar) = jarTasks[config.publicationName]!!
                    artifact(sourcesJar)
                    artifact(javadocJar)

                    this.groupId = groupId
                    artifactId = config.artifactId
                    version = versionTag
                    setupPom(config.displayName)
                }
            }
        }

        repositories {
            maven {
                name = "Local"
                url = uri(layout.buildDirectory.dir("repos/bundles").get().asFile.toURI())
            }
        }
    }
}

signing {
    val signingKey = System.getenv("GPG_PRIVATE_KEY") ?: ""
    val signingPassword = System.getenv("GPG_PASSPHRASE") ?: ""
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}

val username = System.getenv("CENTRAL_USERNAME") ?: ""
val password = System.getenv("CENTRAL_PASSWORD") ?: ""

mavenCentral {
    repoDir = layout.buildDirectory.dir("repos/bundles")
    authToken = if (username.isNotEmpty() && password.isNotEmpty()) {
        Base64.getEncoder().encodeToString("$username:$password".toByteArray())
    } else {
        System.getenv("CENTRAL_TOKEN") ?: ""
    }

    publishingType = "USER_MANAGED"
    maxWait = 500
}

tasks.register("publishAllToMavenLocal") {
    dependsOn("publishAlgorandFoundationCryptoPublicationToMavenLocal")
    dependsOn("publishAlgorandFoundationProviderPublicationToMavenLocal")
}

// Helper function to configure POM metadata
fun MavenPublication.setupPom(libName: String) {
    pom {
        packaging = "aar"
        this.name.set(libName)
        this.description.set("$libName: Android Foundation Libraries")
        this.url.set("https://github.com/michaeltchuang/upload-aar-to-mavencentral")
        this.inceptionYear.set("2025")

        licenses {
            license {
                this.name.set("The Apache License, Version 2.0")
                this.url.set("https://github.com/michaeltchuang/upload-aar-to-mavencentral/blob/main/LICENSE")
            }
        }

        developers {
            developer {
                this.id.set("michaeltchuang")
                this.name.set("Michael T Chuang")
                this.email.set("hello@michaeltchuang.com")
            }
        }

        scm {
            this.connection.set("scm:git:git://github.com/michaeltchuang/upload-aar-to-mavencentral.git")
            this.developerConnection.set("scm:git:ssh://git@github.com/michaeltchuang/upload-aar-to-mavencentral.git")
            this.url.set("https://github.com/michaeltchuang/upload-aar-to-mavencentral")
        }
    }
}
