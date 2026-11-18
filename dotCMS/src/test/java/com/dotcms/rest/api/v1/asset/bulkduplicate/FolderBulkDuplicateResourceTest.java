package com.dotcms.rest.api.v1.asset.bulkduplicate;

import static org.junit.Assert.assertTrue;

import io.swagger.v3.oas.annotations.Operation;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.Test;

/**
 * Unit tests for what {@link FolderBulkDuplicateResource} documents (#37062, US2, FR-009a).
 * <p>
 * The outcome never reports the duplicate's name, so the API description is where a client learns
 * the naming rule. The generated OpenAPI text is built from this {@link Operation}, so this reads
 * the annotation rather than the generated file.
 */
public class FolderBulkDuplicateResourceTest {

    private static String description() {
        final Method method = Arrays.stream(FolderBulkDuplicateResource.class.getMethods())
                .filter(candidate -> candidate.getName().equals("bulkDuplicate"))
                .findFirst()
                .orElseThrow();
        return method.getAnnotation(Operation.class).description();
    }

    /**
     * Method to test: the {@code @Operation} on {@link FolderBulkDuplicateResource#bulkDuplicate}
     * Given Scenario: A client reading the API description
     * ExpectedResult: It states the rule: the source name followed by _copy, appended again until
     * the name is free
     */
    @Test
    public void test_description_statesTheNamingRule() {
        final String description = description();

        assertTrue(description, description.contains("followed by `_copy`"));
        assertTrue(description, description.contains("appended again until the name is free"));
    }

    /**
     * Method to test: the {@code @Operation} on {@link FolderBulkDuplicateResource#bulkDuplicate}
     * Given Scenario: A client deciding what to look for after a run
     * ExpectedResult: It says the outcome identifies each folder by the submitted path, since the
     * duplicate's name is not reported
     */
    @Test
    public void test_description_saysTheOutcomeUsesTheSubmittedPath() {
        assertTrue(description().contains("by the path that was submitted"));
    }
}
