dependencies {
    implementation(platform(project(":binbun-bom")))
    api(project(":binbun-acp-protocol"))
    api(project(":binbun-acp-auth"))
    api(project(":binbun-acp-transport-socket"))
    implementation(project(":binbun-gateway"))
    implementation(project(":binbun-delivery-webhook"))
    implementation(project(":binbun-gateway-health"))
    implementation(project(":binbun-gateway-recovery"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
