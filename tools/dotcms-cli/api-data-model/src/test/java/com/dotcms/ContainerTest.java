package com.dotcms;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(ContainerResource.class)
public class ContainerTest {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ContainerTest.class);
    private static final String BASE_URI = "http://localhost";
    private static final String DOTCMS_PORT = "8080";
    private static final String ELASTICSEARCH_PORT = "9200";

    private static final int STATUS_CODE_OK = 200;

    /*
     * TODO: Parameterize
     * */
    @Test
    void test_dotcms_container_up() {
        LOGGER.info("test_dotcms_container_up");

        given()
                .when().get(BASE_URI + ":" + DOTCMS_PORT + "/dotAdmin")
                .then().log().all()
                .statusCode(STATUS_CODE_OK);
    }

    @Test
    void test_elasticsearch_container_up() {
        LOGGER.info("test_elasticsearch_container_up");

        given()
                .when().get(BASE_URI + ":" + ELASTICSEARCH_PORT + "/")
                .then().log().all()
                .statusCode(STATUS_CODE_OK);
    }
}

