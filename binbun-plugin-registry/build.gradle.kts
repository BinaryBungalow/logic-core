dependencies {
    implementation(platform(project(":binbun-bom")))
    api(project(":binbun-plugin-manifest"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
