dependencies {
    implementation(platform(project(":binbun-bom")))
    api(project(":binbun-plugin-manifest"))
    api(project(":binbun-plugin-runtime"))
    api(project(":binbun-tools"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
