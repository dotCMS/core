package com.dotcms.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotmarketing.util.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link SearchView} carries the warnings of a search result and never exposes a null or
 * modifiable list.
 */
public class SearchViewTest {

    /**
     * Method to test: {@link SearchView#getWarnings()}
     * Given scenario: a view built with the four-argument constructor
     * Expected result: no warnings
     */
    @Test
    public void defaultWarningsAreEmpty() {
        final SearchView view = new SearchView(1, 2, 3, new JsonObjectView(new JSONObject()));

        assertTrue(view.getWarnings().isEmpty());
    }

    /**
     * Method to test: {@link SearchView#getWarnings()}
     * Given scenario: a null warnings list
     * Expected result: no warnings, never null
     */
    @Test
    public void nullWarningsBecomeEmpty() {
        final SearchView view = new SearchView(1, 2, 3, new JsonObjectView(new JSONObject()), null);

        assertTrue(view.getWarnings().isEmpty());
    }

    /**
     * Method to test: {@link SearchView#getWarnings()}
     * Given scenario: a warnings list that the caller keeps modifying
     * Expected result: the view keeps its own copy and does not let callers change it
     */
    @Test
    public void warningsAreCopiedAndReadOnly() {
        final List<String> warnings = new ArrayList<>(List.of("one"));
        final SearchView view = new SearchView(1, 2, 3, new JsonObjectView(new JSONObject()), warnings);
        warnings.add("two");

        assertEquals(List.of("one"), view.getWarnings());
        assertThrows(UnsupportedOperationException.class, () -> view.getWarnings().add("three"));
    }

}
