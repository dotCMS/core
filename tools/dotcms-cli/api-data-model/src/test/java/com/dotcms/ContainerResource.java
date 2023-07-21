package com.dotcms;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class ContainerResource implements QuarkusTestResourceLifecycleManager {

    private static final int POSTGRES_SERVICE_PORT = 5432;
    private static final int ELASTICSEARCH_SERVICE_PORT = 9200;
    private static final int DOTCMS_SERVICE_PORT = 8080;
    private static final int STARTUP_TIMEOUT = 150;
    private static final boolean LOCAL_COMPOSE = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerResource.class);

    static DockerComposeContainer<?> COMPOSE_CONTAINER; // need to be false to run on macOS

    static {
        DockerComposeContainer dockerComposeContainer = new DockerComposeContainer("dotcms-env", new File("src/test/resources/docker-compose.yaml"));
        dockerComposeContainer.withExposedService("postgres", POSTGRES_SERVICE_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT)));
        dockerComposeContainer.withExposedService("elasticsearch", ELASTICSEARCH_SERVICE_PORT, Wait.forHttp("/").forPort(ELASTICSEARCH_SERVICE_PORT).forStatusCode(200));
        dockerComposeContainer.withExposedService("dotcms", DOTCMS_SERVICE_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT)));
//        dockerComposeContainer.withExposedService("dotcms", DOTCMS_SERVICE_PORT, Wait.forHttp("/dotAdmin").forPort(DOTCMS_SERVICE_PORT).forStatusCode(200));
//        dockerComposeContainer.withLogConsumer("dotcms", new Slf4jLogConsumer(LOGGER));
        dockerComposeContainer.withLocalCompose(LOCAL_COMPOSE);//
        COMPOSE_CONTAINER = dockerComposeContainer;
    }

    @Override
    public Map<String, String> start() {
        COMPOSE_CONTAINER.start();
        final Map<String, String> conf = new HashMap<>();
        conf.put("%test.dotcms.url", COMPOSE_CONTAINER.getServiceHost("dotcms", DOTCMS_SERVICE_PORT) + ":" + COMPOSE_CONTAINER.getServicePort("dotcms", DOTCMS_SERVICE_PORT));

        return conf;
    }

    @Override
    public void stop() {
        COMPOSE_CONTAINER.stop();
    }
}
