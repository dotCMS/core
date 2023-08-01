package com.dotcms;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(ContainerResource.class)
public class ContainerTest {
}

