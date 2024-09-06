package com.dotcms;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * ContainerResource implements the QuarkusTestResourceLifecycleManager interface to manage starting and stopping the Docker containers for testing.
 * The class has constants for the ports of the Postgres, Elasticsearch, and dotCMS services exposed by Docker Compose.
 * The static initializer block reads test configuration from MicroProfile Config and creates a DockerComposeContainer.
 * It configures exposed services, environment variables, and logging.
 * The start() method starts the Docker Compose container and returns a map with the dotCMS URL for tests to use.
 * The stop() method stops the Docker Compose container when tests are complete.
 */
public class ContainerResource implements QuarkusTestResourceLifecycleManager {

    private boolean testcontainersEnabled;
    private int dotcmsServicePort;

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerResource.class);



    DockerComposeContainer<?> COMPOSE_CONTAINER;

    @Override
    public void init(Map<String, String> initArgs) {
        // CDI context not fully ready so we need to get config from static provider
        Config config = ConfigProvider.getConfig();
        testcontainersEnabled = Boolean.parseBoolean(config.getValue("testcontainers.enabled", String.class));

        int postgresServicePort = Integer.parseInt(
                config.getValue("testcontainers.postgres.service.port", String.class));
        int elasticsearchServicePort = Integer.parseInt(
                config.getValue("testcontainers.elasticsearch.service.port", String.class));
        dotcmsServicePort = Integer.parseInt(config.getValue("testcontainers.dotcms.service.port", String.class));

        final boolean isLoggerEnabled = Boolean.parseBoolean(config.getValue("testcontainers.logger.enabled", String.class));
        final boolean isLocalComposeEnabled = Boolean.parseBoolean(config.getValue("testcontainers.docker.compose.local.enabled", String.class));
        final String dockerImage = config.getValue("testcontainers.docker.image", String.class);
        final String dockerComposeFile = config.getValue("testcontainers.docker.compose.file", String.class);

        final String dotcmsLicenseFile = config.getValue("testcontainers.dotcms.license.file", String.class);
        final long dockerComposeStartupTimeout = config.getValue("testcontainers.docker.compose.startup.timeout", Long.class);

        DockerComposeContainer<?> dockerComposeContainer = new DockerComposeContainer<>("dotcms-env", new File(dockerComposeFile));
        dockerComposeContainer.withExposedService("postgres",
                postgresServicePort, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(dockerComposeStartupTimeout)));
        dockerComposeContainer.withExposedService("elasticsearch",
                elasticsearchServicePort, Wait.forHttp("/").forPort(elasticsearchServicePort).forStatusCode(HttpStatus.SC_OK));
        dockerComposeContainer.withExposedService("dotcms",
                dotcmsServicePort, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(dockerComposeStartupTimeout)));
        dockerComposeContainer.withEnv("DOTCMS_IMAGE", dockerImage);
        dockerComposeContainer.withEnv("DOTCMS_LICENSE_FILE", dotcmsLicenseFile);
        dockerComposeContainer.withPull(false);
        dockerComposeContainer.withTailChildContainers(true);

        if (isLoggerEnabled) {
            dockerComposeContainer.withLogConsumer("dotcms", new Slf4jLogConsumer(LOGGER));
        }

        dockerComposeContainer.withLocalCompose(isLocalComposeEnabled); // Needs to be false to run in macOS
        COMPOSE_CONTAINER = dockerComposeContainer;
    }


    @Override
    public Map<String, String> start() {

        if (!testcontainersEnabled) {
            LOGGER.info("Testcontainers are disabled. Skipping container startup.");
            return new HashMap<>();
        }

        COMPOSE_CONTAINER.start();
        final Map<String, String> conf = new HashMap<>();
        conf.put("%test.dotcms.url", COMPOSE_CONTAINER.getServiceHost("dotcms", dotcmsServicePort) + ":" + COMPOSE_CONTAINER.getServicePort("dotcms",
                dotcmsServicePort));

        return conf;
    }

    @Override
    public void stop() {
        if (!testcontainersEnabled) {
            LOGGER.info("Testcontainers are disabled. Skipping container shutdown.");
            return;
        }
        COMPOSE_CONTAINER.stop();
    }
}
