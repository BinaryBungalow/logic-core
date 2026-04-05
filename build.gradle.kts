plugins {
    `java-library`
}

allprojects {
    group = "ai.binbun"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    if (name != "binbun-bom") {
        apply(plugin = "java-library")

        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
            withSourcesJar()
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        dependencies {
            testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
        }

        dependencies {
            testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
        }
    }
}
