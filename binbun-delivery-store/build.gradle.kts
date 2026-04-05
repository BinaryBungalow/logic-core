dependencies {
    implementation(platform(project(":binbun-bom")))
    api(project(":binbun-delivery-model"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
