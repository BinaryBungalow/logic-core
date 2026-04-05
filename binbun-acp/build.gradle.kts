dependencies {
    implementation(platform(project(":binbun-bom")))
    api(project(":binbun-agent-core"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
