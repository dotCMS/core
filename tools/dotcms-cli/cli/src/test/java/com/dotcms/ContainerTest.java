package com.dotcms;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;


@QuarkusTest
@QuarkusTestResource(ContainerResource.class)
public class ContainerTest {

    @Test
    void test_dotcms_container_up() {
        System.out.println("test_dotcms_container_up");

        given()
          .when().get("http://localhost:8080/dotAdmin")
          .then()
             .statusCode(200);
    }

    @Test
    void test_elasticsearch_container_up() {
        System.out.println("test_elasticsearch_container_up");

        given()
                .when().get("http://localhost:9200/")
                .then()
                .statusCode(200);
    }
}
