import java.util.Base64

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("signing")
    id("tech.yanand.maven-central-publish") version "1.3.0"
}

android {
    namespace = "com.michaeltchuang.algokit"
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
    compileSdk = libs.versions.android.compileSdk.get().toInt()
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}

afterEvaluate {
    val versionTag = System.getenv("VERSION_TAG") ?: "0.1.0"
    publishing {
        publications {
            create<MavenPublication>("AlgorandFoundationCrypto") {
                artifact(file("crypto-debug.aar")) {
                    extension = "aar"
                }

                artifact(tasks.named("sourcesJar"))
                artifact(tasks.named("javadocJar"))

                groupId = "com.michaeltchuang.algokit"
                artifactId = "algorand-foundation-crypto"
                version = versionTag
                setupPom("AlgorandFoundationCrypto")
            }

            create<MavenPublication>("AlgorandFoundationProvider") {
                artifact(file("provider-debug.aar")) {
                    extension = "aar"
                }

                artifact(tasks.named("sourcesJar"))
                artifact(tasks.named("javadocJar"))

                groupId = "com.michaeltchuang.algokit"
                artifactId = "algorand-foundation-provider"
                version = versionTag
                setupPom("AlgorandFoundationProvider")
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
    // About GPG signing, please refer to https://central.sonatype.org/publish/requirements/gpg/
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
