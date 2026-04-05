dependencies {
    implementation(platform(project(":binbun-bom")))
    api(project(":binbun-acp-protocol"))
    api(project(":binbun-acp-auth"))
    implementation(project(":binbun-gateway"))
    implementation(project(":binbun-tools"))
    implementation(project(":binbun-memory"))
    implementation(project(":binbun-workflows"))
    implementation(project(":binbun-agent-core"))
    implementation(project(":binbun-model"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}
