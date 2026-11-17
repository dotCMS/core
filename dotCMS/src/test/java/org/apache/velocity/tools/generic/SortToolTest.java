package org.apache.velocity.tools.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.liferay.portal.model.Company;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link SortTool} sorts by bean properties, except by a property that reads the company key.
 */
class SortToolTest {

    /**
     * Method to test: {@link SortTool#sort(Object, String)}.
     * Given scenario: a template sorts a company and a map holding a value under {@code key}, by {@code key}.
     * Expected result: the sort is refused, so the order cannot tell how the key compares to the value.
     */
    @Test
    void sort_byCompanyKey_isRefused() {
        final Company company = new Company("sample.company");
        company.setKeyObj(new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES"));
        final List<Object> items = new ArrayList<>(List.of(company, Map.of("key", "rO0AB")));

        assertNull(new SortTool().sort(items, "key"));
    }

    /**
     * Method to test: {@link SortTool#sort(Object, String)}.
     * Given scenario: a template sorts ordinary maps by a property.
     * Expected result: the sorted list is returned as before.
     */
    @Test
    void sort_byOrdinaryProperty_sorts() {
        final Collection<?> sorted = new SortTool().sort(
                new ArrayList<>(List.of(Map.of("name", "b"), Map.of("name", "a"))), "name");

        assertEquals(List.of(Map.of("name", "a"), Map.of("name", "b")), sorted);
    }
}
