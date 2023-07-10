//package com.dotcms;
//
//import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
//import org.testcontainers.containers.DockerComposeContainer;
//import org.testcontainers.containers.wait.strategy.Wait;
//import org.testcontainers.lifecycle.Startables;
//
//import java.io.File;
//import java.time.Duration;
//import java.util.Collections;
//import java.util.Map;
//import java.util.stream.Stream;
//
//public class DockerComposeTestResource implements QuarkusTestResourceLifecycleManager {
//
//    private static final DockerComposeContainer<?> composeContainer = new DockerComposeContainer<>(new File("src/test/resources/docker-compose.yaml"))
//            .withLocalCompose(true)
//
//            .withServices("postgres");
//
//    static {
//        Startables.deepStart(Stream.of(composeContainer)).join();
//    }
//    @Override
//    public Map<String, String> start() {
//        return Map.of(
//                "quarkus.datasource.jdbc.url", composeContainer.getServiceHost("postgres", 5432)
//                        + ":" + composeContainer.getServicePort("postgres", 5432),
//                "quarkus.datasource.username", "your_username",
//                "quarkus.datasource.password", "your_password"
//        );
//    }
//
//    @Override
//    public void stop() {
//        // No explicit stop needed as Testcontainers manages the lifecycle
//    }
//
//
////    private static final int SERVICE_PORT = 5432;
////    private static final int STARTUP_TIMEOUT = 60;
////    private static final DockerComposeContainer<?> DOCKER_COMPOSE_CONTAINER =
////            new DockerComposeContainer<>(new File("src/test/resources/docker-compose.yaml"))
////                    .withExposedService(
////                            "dotcms-env",
////                            SERVICE_PORT,
////                            Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT)))
////                    .withLocalCompose(true);
////
////    @Override
////    public Map<String, String> start() {
////        DOCKER_COMPOSE_CONTAINER.start();
////        String dotcmsServiceHost = DOCKER_COMPOSE_CONTAINER.getServiceHost("dotcms-env", SERVICE_PORT);
////        String dotcmsServicePort = DOCKER_COMPOSE_CONTAINER.getServicePort("dotcms-env", SERVICE_PORT).toString();
////
////        return Collections.singletonMap("dotcms.service.url", "http://" + dotcmsServiceHost + ":" + dotcmsServicePort);
////    }
////
////    @Override
////    public void stop() {
////        DOCKER_COMPOSE_CONTAINER.stop();
////    }
//}
