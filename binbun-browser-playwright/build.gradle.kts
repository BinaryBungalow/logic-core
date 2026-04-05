dependencies {
    implementation(platform(project(":binbun-bom")))
    api(project(":binbun-browser-core"))
    implementation("com.microsoft.playwright:playwright:1.48.0")
}
