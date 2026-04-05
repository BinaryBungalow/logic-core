dependencies {
    implementation(platform(project(":binbun-bom")))
    api(project(":binbun-delivery-core"))
    implementation(project(":binbun-delivery-store"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
