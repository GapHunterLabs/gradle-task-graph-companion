tasks.register("publishContract") {
    // Accidental copy-paste from api-gateway's build script -- this
    // creates a real cycle: api-gateway:generateClientStubs depends on
    // billing-service:publishContract, which depends right back on it.
    dependsOn(":api-gateway:generateClientStubs")
}

tasks.register("buildImage") {
    dependsOn("publishContract")
}
