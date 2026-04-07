plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "logic-core"

include(
    ":binbun-bom",
    ":binbun-model",
    ":binbun-agent-core",
    ":binbun-tools",
    ":binbun-memory",
    ":binbun-resources",
    ":binbun-cli",
    ":binbun-deploy",
    ":binbun-gateway",
    ":binbun-core-plugin",
    ":binbun-tooling-native",
    ":binbun-workflows",
    ":binbun-acp",
    ":binbun-acp-protocol",
    ":binbun-acp-auth",
    ":binbun-acp-transport-socket",
    ":binbun-acp-transport-http",
    ":binbun-delivery-core",
    ":binbun-delivery-model",
    ":binbun-delivery-store",
    ":binbun-delivery-webhook",
    ":binbun-delivery-telegram",
    ":binbun-delivery-slack",
    ":binbun-plugin-manifest",
    ":binbun-plugin-resolver",
    ":binbun-plugin-runtime",
    ":binbun-plugin-package",
    ":binbun-plugin-registry",
    ":binbun-gateway-health",
    ":binbun-gateway-observability",
    ":binbun-gateway-recovery",
    ":binbun-browser-core",
    ":binbun-browser-playwright",
    ":binbun-integration-tests"
)
