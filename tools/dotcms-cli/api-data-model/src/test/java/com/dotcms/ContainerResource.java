package com.dotcms;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpStatus;
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

@QuarkusTest
public class ContainerResource implements QuarkusTestResourceLifecycleManager {

    private static final int POSTGRES_SERVICE_PORT = 5432;
    private static final int ELASTICSEARCH_SERVICE_PORT = 9200;
    private static final int DOTCMS_SERVICE_PORT = 8080;
    private static final long DEFAULT_STARTUP_TIMEOUT = 150;
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerResource.class);

    static DockerComposeContainer<?> COMPOSE_CONTAINER;

    static {

        final boolean isLoggerEnabled = Boolean.parseBoolean(ConfigProvider.getConfig().getValue("testcontainers.logger.enabled", String.class));
        final boolean isLocalComposeEnabled = Boolean.parseBoolean(ConfigProvider.getConfig().getValue("testcontainers.docker.compose.local.enabled", String.class));
        final String dockerImage = ConfigProvider.getConfig().getValue("testcontainers.docker.image", String.class);
        final String dockerComposeFile = ConfigProvider.getConfig().getValue("testcontainers.docker.compose.file", String.class);
        final String dotcmsLicenseFile = ConfigProvider.getConfig().getValue("testcontainers.dotcms.license.file", String.class);
        final long dockerComposeStartupTimeout = ConfigProvider.getConfig().getOptionalValue("testcontainers.docker.compose.startup.timeout", Long.class).orElse(DEFAULT_STARTUP_TIMEOUT);

        DockerComposeContainer dockerComposeContainer = new DockerComposeContainer("dotcms-env", new File(dockerComposeFile));
        dockerComposeContainer.withExposedService("postgres", POSTGRES_SERVICE_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(dockerComposeStartupTimeout)));
        dockerComposeContainer.withExposedService("elasticsearch", ELASTICSEARCH_SERVICE_PORT, Wait.forHttp("/").forPort(ELASTICSEARCH_SERVICE_PORT).forStatusCode(HttpStatus.SC_OK));
        dockerComposeContainer.withExposedService("dotcms", DOTCMS_SERVICE_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(dockerComposeStartupTimeout)));
        dockerComposeContainer.withEnv("DOTCMS_IMAGE", dockerImage);
        dockerComposeContainer.withEnv("DOTCMS_LICENSE_FILE", dotcmsLicenseFile);

        if (isLoggerEnabled) {
            dockerComposeContainer.withLogConsumer("dotcms", new Slf4jLogConsumer(LOGGER));
        }

        dockerComposeContainer.withLocalCompose(isLocalComposeEnabled); // Needs to be false to run in macOS
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
