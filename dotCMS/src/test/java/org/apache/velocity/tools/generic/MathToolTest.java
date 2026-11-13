package org.apache.velocity.tools.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liferay.portal.model.Company;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link MathTool} totals bean properties by path, except a path that reads the company key.
 */
class MathToolTest {

    /**
     * Method to test: {@link MathTool#getTotal(java.util.Collection, String)}.
     * Given scenario: a template totals a path that reads into the company key.
     * Expected result: no total is returned.
     */
    @Test
    void getTotal_pathThroughCompanyKey_returnsNothing() {
        final Company company = new Company("sample.company");
        company.setKeyObj(new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES"));

        // Checked without printing the total, so a failure does not write part of a key to the build log
        assertTrue(new MathTool().getTotal(List.of(company), "key.bytes[0]") == null,
                "the total must not read the key");
        assertTrue(new MathTool().getTotal(List.of(company), "keyObj.encoded[0]") == null,
                "the total must not read the key object");
    }

    /**
     * Method to test: {@link MathTool#getTotal(java.util.Collection, String)}.
     * Given scenario: a template totals an ordinary numeric value, by property and by nested path.
     * Expected result: the total is returned as before.
     */
    @Test
    void getTotal_ordinaryPath_returnsTheTotal() {
        final List<Map<String, Object>> rows = List.of(Map.of("price", 2), Map.of("price", 3));

        assertEquals(5, new MathTool().getTotal(rows, "price"));
        assertEquals(5, new MathTool().getTotal(
                List.of(Map.of("item", Map.of("price", 2)), Map.of("item", Map.of("price", 3))), "item.price"));
    }
}
