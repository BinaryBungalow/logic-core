plugins {
    `java-library`
}

dependencies {
    implementation(platform(project(":binbun-bom")))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
