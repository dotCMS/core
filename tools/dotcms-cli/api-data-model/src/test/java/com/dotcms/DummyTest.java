//package com.dotcms;
//
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.testcontainers.containers.DockerComposeContainer;
//import org.testcontainers.containers.wait.strategy.Wait;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.io.File;
//import java.time.Duration;
//
//@Testcontainers
//public class DummyTest {
//
//    private static final int POSTGRES_SERVICE_PORT = 5432;
//    private static final int ELASTICSEARCH_SERVICE_PORT = 9200;
//    private static final int DOTCMS_SERVICE_PORT = 8080;
//    private static final int STARTUP_TIMEOUT = 120;
//
//    private static final DockerComposeContainer<?> COMPOSE_CONTAINER =
//            new DockerComposeContainer("dotcms-env", new File("src/test/resources/docker-compose.yaml"))
//                    .withExposedService("postgres", POSTGRES_SERVICE_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT)))
//                    .withExposedService("elasticsearch", ELASTICSEARCH_SERVICE_PORT, Wait.forHttp("/").forPort(ELASTICSEARCH_SERVICE_PORT).forStatusCode(200))
//                    .withExposedService("dotcms", DOTCMS_SERVICE_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT)))
//                    .withLocalCompose(false); // need to be false to run on macOS
//
//    static {
//        COMPOSE_CONTAINER.start();
////        Stream.of(GENERIC_CONTAINER, COMPOSE_CONTAINER).parallel().forEach(Startable::start);
////        Startables.deepStart(Stream.of(GENERIC_CONTAINER, COMPOSE_CONTAINER)).join();
//    }
//
//    @BeforeAll
//    public static void beforeAll() {
//        System.out.println("beforeAll");
//
//        System.out.println("Postgres address: " + COMPOSE_CONTAINER.getServiceHost("postgres", POSTGRES_SERVICE_PORT));
//        System.out.println("Postgres port: " + COMPOSE_CONTAINER.getServicePort("postgres", POSTGRES_SERVICE_PORT));
//
//        System.out.println("Elasticsearch address: " + COMPOSE_CONTAINER.getServiceHost("elasticsearch", ELASTICSEARCH_SERVICE_PORT));
//        System.out.println("Elasticsearch port: " + COMPOSE_CONTAINER.getServicePort("elasticsearch", ELASTICSEARCH_SERVICE_PORT));
//
//        System.out.println("DotCMS address: " + COMPOSE_CONTAINER.getServiceHost("dotcms", DOTCMS_SERVICE_PORT));
//        System.out.println("DotCMS port: " + COMPOSE_CONTAINER.getServicePort("dotcms", DOTCMS_SERVICE_PORT));
//
//    }
//
//    @Test
//    public void myTest() {
//        // Your test logic here
//        System.out.println("Hello World");
//    }
//
//
////    private static final Network NETWORK = Network.newNetwork();
////
////    @Container
////    private static final ElasticsearchContainer ELASTICSEARCH_CONTAINER =
////            new ElasticsearchContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.10.2"))
////                    .withEnv("discovery.type", "single-node")
////                    .withEnv("cluster.name", "elastic-cluster")
////                    .withEnv("bootstrap.memory_lock", "true")
////                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx1G")
////                    .withExposedPorts(ELASTICSEARCH_SERVICE_PORT, 9600)
////                    .waitingFor(Wait.forHttp("/").forPort(ELASTICSEARCH_SERVICE_PORT).forStatusCode(200))
////                    .withNetwork(NETWORK)
////                    .withLogConsumer(new Slf4jLogConsumer(org.slf4j.LoggerFactory.getLogger("elasticsearch")))
////                    .withReuse(true);
//
//
//
//
//
////    @Container
////    public static final DockerComposeContainer<?> dotCmsEnv = new DockerComposeContainer(new File("src/test/resources/docker-compose.yaml"))
////            .withExposedService("dotcms", POSTGRES_SERVICE_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT)))
////            .withLocalCompose(true);
//
////    @Container
////    private static final DockerComposeContainer<?> dotCmsEnv = new DockerComposeContainer(new File("src/test/resources/docker-compose.yaml"))
////            .withExposedService("dotcms", 8080, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))
////            .withLocalCompose(true);
//
////    @Container
////    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
////            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
//////                    .withDatabaseName("dotcms")
//////                    .withUsername("dotcms")
//////                    .withPassword("dotcms")
////                    .withEnv("POSTGRES_USER", "dotcms")
////                    .withEnv("POSTGRES_PASSWORD", "dotcms")
////                    .withEnv("POSTGRES_DB", "dotcms")
////                    .withCommand("postgres -c 'max_connections=400' -c 'shared_buffers=128MB'")
////                    .withExposedPorts(POSTGRES_SERVICE_PORT)
////                    .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT)))
////                    .withNetwork(NETWORK)
////                    .withReuse(true);
//
//
////    @BeforeAll
////    public static void setup() {
////
//////        System.out.println("Postgres address: " + POSTGRES_CONTAINER.getHost());
//////        System.out.println("Postgres port: " + POSTGRES_CONTAINER.getFirstMappedPort());
//////
//////        System.out.println("Elasticsearch address: " + ELASTICSEARCH_CONTAINER.getHost());
//////        System.out.println("Elasticsearch port: " + ELASTICSEARCH_CONTAINER.getFirstMappedPort());
//////
//////        GenericContainer<?> DOTCMS_CONTAINER = new GenericContainer<>(DockerImageName.parse("dotcms/dotcms:latest"))
//////                .withEnv("CMS_JAVA_OPTS", "-Xmx1G")
//////                .withEnv("TZ", "UTC")
//////                .withEnv("DB_BASE_URL", "jdbc:postgresql://" + POSTGRES_CONTAINER.getHost() + ":5432/dotcms")
////////                .withEnv("DB_BASE_URL", "jdbc:postgresql://" + POSTGRES_CONTAINER.getHost() + ":" + POSTGRES_CONTAINER.getFirstMappedPort() + "/dotcms")
//////                .withEnv("DB_USERNAME", "dotcms")
//////                .withEnv("DB_PASSWORD", "dotcms")
//////                .withEnv("DOT_ES_AUTH_BASIC_PASSWORD", "admin")
//////                .withEnv("DOT_INITIAL_ADMIN_PASSWORD", "admin")
//////                .withEnv("DOT_ES_ENDPOINTS", "https://" + ELASTICSEARCH_CONTAINER.getHost() + ":9200")
////////                .withEnv("DOT_ES_ENDPOINTS", "https://" + ELASTICSEARCH_CONTAINER.getHost() + ":" + ELASTICSEARCH_CONTAINER.getFirstMappedPort())
////////                    .withEnv("CUSTOM_STARTER_URL", "https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/20211201/starter-20211201.zip")
//////                .withExposedPorts(8080, 8443)
//////                .waitingFor(Wait.forHttp("/dotAdmin").forPort(8080).forStatusCode(200))
//////                .dependsOn(POSTGRES_CONTAINER, ELASTICSEARCH_CONTAINER)
//////                .withNetwork(NETWORK)
//////                .withLogConsumer(logger -> System.out.print(logger.getUtf8String()))
//////                .withReuse(true);
//////
//////        DOTCMS_CONTAINER.start();
////
//////        System.out.println("DotCMS address: " + DOTCMS_CONTAINER.getHost());
//////        System.out.println("DotCMS port: " + DOTCMS_CONTAINER.getFirstMappedPort());
////
////        System.out.println("DummyTest.setup");
////
////    }
//
////    @Test
////    public void should_launch_containers_test() {
////        System.out.println("DummyTest.test");
////    }
//}
