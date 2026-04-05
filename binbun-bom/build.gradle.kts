plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api("info.picocli:picocli:4.7.7")
        api("com.fasterxml.jackson.core:jackson-databind:2.17.2")
        api("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
    }
}
