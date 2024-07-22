package com.dotcms.api.client.push.contenttype;

import com.dotcms.DotCMSITProfile;
import com.dotcms.cli.common.ContentTypesTestHelperService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class ContentTypeValidatorTest {

    @Inject
    ContentTypesTestHelperService contentTypesTestHelper;

    private static final String TEST_IDENTIFIER = "bbe0293638b685eb422dab5766441b3b";
    private static final String TEST_VARIABLE = "test";

    /**
     * Given scenario: The content type is null.
     * Expected result: The validate method should throw a NullPointerException.
     */
    @Test
    void validate_ShouldThrowException_WhenContentTypeIsNull() {
        // Given a null content type
        // When validating the content type
        Executable executable = () -> ContentTypeValidator.validate(null);

        // Then a NullPointerException should be thrown
        assertThrows(NullPointerException.class, executable,
                "ContentType must not be null.");
    }

    /**
     * Given scenario: The content type has more than four columns.
     * Expected result: The validate method should throw an IllegalArgumentException.
     */
    @Test
    void validate_ShouldThrowException_WhenRowsExceedColumnLimit() {
        // Given a content type with more than four columns
        var contentType = contentTypesTestHelper.buildContentTypeWithColumns(
                TEST_IDENTIFIER, TEST_VARIABLE, 5);

        // When validating the content type
        Executable executable = () -> ContentTypeValidator.validate(contentType);

        // Then an IllegalArgumentException should be thrown
        assertThrows(IllegalArgumentException.class, executable,
                "The maximum number of columns per row is limited to four.");
    }

    /**
     * Given scenario: The content type has four or fewer columns.
     * Expected result: The validate method should not throw any exception.
     */
    @Test
    void validate_ShouldNotThrowException_WhenRowsAreWithinColumnLimit() {
        // Given a content type with four or fewer columns
        var contentType = contentTypesTestHelper.buildContentTypeWithColumns(
                TEST_IDENTIFIER, TEST_VARIABLE, 4);

        // When validating the content type
        // Then no exception should be thrown
        assertDoesNotThrow(() -> ContentTypeValidator.validate(contentType));
    }

    /**
     * Given scenario: The content type has zero columns.
     * Expected result: The validate method should not throw any exception.
     */
    @Test
    void validate_ShouldNotThrowException_WhenLayoutColumnsIsZero() {
        // Given a content type with zero columns
        var contentType = contentTypesTestHelper.buildContentTypeWithColumns(
                TEST_IDENTIFIER, TEST_VARIABLE, 0);

        // When validating the content type
        // Then no exception should be thrown
        assertDoesNotThrow(() -> ContentTypeValidator.validate(contentType));
    }

    /**
     * Given scenario: The content type has no layout columns.
     * Expected result: The validate method should not throw any exception.
     */
    @Test
    void validate_ShouldNotThrowException_WhenLayoutIsNull() {
        // Given a content type without layout columns
        var contentType = contentTypesTestHelper.buildContentTypeWithColumnsWithoutLayout(
                TEST_IDENTIFIER, TEST_VARIABLE, 4);

        // When validating the content type
        // Then no exception should be thrown
        assertDoesNotThrow(() -> ContentTypeValidator.validate(contentType));
    }
}
