dependencies {
    implementation(platform(project(":binbun-bom")))
    api(project(":binbun-model"))
    api(project(":binbun-tools"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}
