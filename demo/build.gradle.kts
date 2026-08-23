// Root aggregation tasks -- realistic for a multi-service platform build.
tasks.register("generateOpenApiSpec") { }

tasks.register("publishAllServices") {
    dependsOn("generateOpenApiSpec")
}
