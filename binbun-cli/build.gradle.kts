plugins {
    application
}

application {
    mainClass.set("ai.binbun.cli.Main")
}

dependencies {
    implementation(platform(project(":binbun-bom")))
    implementation(project(":binbun-gateway"))
    implementation(project(":binbun-core-plugin"))
    implementation(project(":binbun-tooling-native"))
    implementation(project(":binbun-workflows"))
    implementation(project(":binbun-memory"))
    implementation(project(":binbun-tools"))
    implementation(project(":binbun-acp"))
    implementation(project(":binbun-acp-auth"))
    implementation(project(":binbun-acp-protocol"))
    implementation(project(":binbun-acp-transport-socket"))
    implementation(project(":binbun-acp-transport-http"))
    implementation(project(":binbun-deploy"))
    implementation(project(":binbun-delivery-store"))
    implementation(project(":binbun-delivery-webhook"))
    implementation(project(":binbun-delivery-telegram"))
    implementation(project(":binbun-delivery-slack"))
    implementation(project(":binbun-browser-core"))
    implementation(project(":binbun-browser-playwright"))
    implementation(project(":binbun-gateway-health"))
    implementation(project(":binbun-gateway-observability"))
    implementation(project(":binbun-gateway-recovery"))
    implementation(project(":binbun-agent-core"))
    implementation("info.picocli:picocli:4.7.6")
    implementation("ai.logicbean:logicbean-tui:0.13.0")
}
