dependencies {
    implementation(platform(project(":binbun-bom")))
    api(project(":binbun-agent-core"))
    api(project(":binbun-workflows"))
    api(project(":binbun-delivery-model"))
    api(project(":binbun-plugin-runtime"))
    api(project(":binbun-plugin-registry"))
}
