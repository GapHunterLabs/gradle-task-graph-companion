tasks.register("generateClientStubs") {
    dependsOn(":billing-service:publishContract")
}

tasks.register("buildImage") {
    dependsOn("generateClientStubs")
}
