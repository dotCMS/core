//package com.dotcms;
//
//import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
//import org.testcontainers.containers.DockerComposeContainer;
//import org.testcontainers.containers.wait.strategy.Wait;
//
//import java.io.File;
//import java.time.Duration;
//import java.util.Collections;
//import java.util.Map;
//
//public class DockerComposeResource implements QuarkusTestResourceLifecycleManager {
//
//    private static final int POSTGRES_SERVICE_PORT = 5432;
//    private static final int ELASTICSEARCH_SERVICE_PORT = 9200;
//    private static final int DOTCMS_SERVICE_PORT = 8080;
//    private static final int STARTUP_TIMEOUT = 120;
//    private static final boolean USE_LOCAL_COMPOSE = false;
//
//    private static final DockerComposeContainer<?> DOCKER_COMPOSE_CONTAINER =
//            new DockerComposeContainer("dotcms-env", new File("src/test/resources/docker-compose.yaml"))
//                    .withExposedService("postgres", POSTGRES_SERVICE_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT)))
//                    .withExposedService("elasticsearch", ELASTICSEARCH_SERVICE_PORT, Wait.forHttp("/").forPort(ELASTICSEARCH_SERVICE_PORT).forStatusCode(200))
//                    .withExposedService("dotcms", DOTCMS_SERVICE_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT)))
//                    .withLocalCompose(USE_LOCAL_COMPOSE); // need to be false to run on macOS
//    @Override
//    public Map<String, String> start() {
//        DOCKER_COMPOSE_CONTAINER.start();
//        String dotcmsServiceHost = DOCKER_COMPOSE_CONTAINER.getServiceHost("dotcms", DOTCMS_SERVICE_PORT);
//        String dotcmsServicePort = DOCKER_COMPOSE_CONTAINER.getServicePort("dotcms", DOTCMS_SERVICE_PORT).toString();
//
//        return Collections.singletonMap("myapp.service.url", "http://" + dotcmsServiceHost + ":" + dotcmsServicePort);
//    }
//
//    @Override
//    public void stop() {
//        DOCKER_COMPOSE_CONTAINER.stop();
//    }
//}