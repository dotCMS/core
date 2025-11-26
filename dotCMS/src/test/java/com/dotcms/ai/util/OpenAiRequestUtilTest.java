package com.dotcms.ai.util;

import com.dotmarketing.util.json.JSONObject;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The OpenAiRequestUtilTest class provides unit tests for the OpenAiRequestUtil class.
 * It includes tests for the truncatePrompt method, covering scenarios where the prompt is shorter, equal to, and longer than the maximum length.
 */
class OpenAiRequestUtilTest {

    private static final String LONGER_THAN_400 = "This is a test prompt that is designed to be longer than the maximum prompt length. " +
            "It includes a lot of unnecessary information and details that are not relevant to the test. " +
            "The purpose of this prompt is to test the truncatePrompt method in the OpenAiRequestUtil class. " +
            "This method is supposed to truncate the prompt to a maximum length. " +
            "Hopefully this test will be enough to cover all possible edge cases.";

    /**
     * Scenario: Truncate a prompt that is longer than the maximum length
     * Given a prompt that is longer than the maximum length
     * When the truncatePrompt method is called
     * Then the returned pair's left value should be true
     * And the length of the returned pair's right value should be less than or equal to the maximum length
     */
    @Test
    void test_truncatePrompt_greaterThan400() {
        final OpenAiRequestUtil util = OpenAiRequestUtil.get();

        final Pair<Boolean, String> truncatedPrompt = util.truncatePrompt(LONGER_THAN_400);

        assertTrue(truncatedPrompt.getLeft());
        assertEquals(397, truncatedPrompt.getRight().length());
    }

    /**
     * Scenario: Truncate a prompt that is shorter than the maximum length
     * Given a prompt that is shorter than the maximum length
     * When the truncatePrompt method is called
     * Then the returned pair's left value should be false
     * And the returned pair's right value should be equal to the original prompt
     */
    @Test
    void test_truncatePrompt_lessThan400() {
        final OpenAiRequestUtil util = OpenAiRequestUtil.get();
        final String prompt = "This is a test prompt that is shorter than the maximum prompt length.";
        final Pair<Boolean, String> truncatedPrompt = util.truncatePrompt(prompt);

        assertFalse(truncatedPrompt.getLeft());
        assertEquals(prompt, truncatedPrompt.getRight());
    }

    /**
     * Scenario: Handle a large prompt by truncating it to a maximum length
     * Given a JSONObject that contains a prompt
     * When the handleLargePrompt method is called
     * Then if the prompt exceeds the maximum length, it should be truncated
     * And the original prompt in the JSONObject should be replaced with the truncated version
     */
    @Test
    void testHandleLargePrompt() {
        final OpenAiRequestUtil util = OpenAiRequestUtil.get();

        final JSONObject promptObject = new JSONObject();
        promptObject.put("prompt", LONGER_THAN_400);

        util.handleLargePrompt(promptObject);

        // Assuming the max length is 400
        Assertions.assertEquals(397, promptObject.getString("prompt").length());
    }

}
