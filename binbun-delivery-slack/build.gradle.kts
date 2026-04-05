dependencies {
    implementation(platform(project(":binbun-bom")))
    api(project(":binbun-delivery-core"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
