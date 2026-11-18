package com.dotcms.graphql;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import graphql.execution.FetchedValue;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UnmatchedTypeConditionInstrumentationTest {

    /**
     * Verifies that a fetched Contentlet without a content type is skipped instead of failing.
     * The Page API hands such synthetic contentlets on (e.g. while resolving a vanity URL); before
     * this was guarded, the resulting NullPointerException failed the whole request with a 500.
     */
    @Test
    void contentletWithoutContentType_isSkippedWithoutFailing() {
        final UnmatchedTypeConditionInstrumentation instrumentation =
                new UnmatchedTypeConditionInstrumentation();
        final UnmatchedTypeConditionInstrumentation.State state = instrumentation.createState();
        state.declare("BannerImages", "page.image");

        // Mocked rather than constructed: a real Contentlet reaches APILocator to resolve its type.
        final Contentlet withoutType = Mockito.mock(Contentlet.class);
        Mockito.when(withoutType.getContentType()).thenReturn(null);
        final FetchedValue wrapped = FetchedValue.newFetchedValue()
                .fetchedValue(List.of(withoutType)).build();

        assertDoesNotThrow(() -> instrumentation.recordResolvedType(wrapped, state));
        assertEquals(1, state.unmatched().size(),
                "a contentlet without a type satisfies no clause, so the clause stays unmatched");
    }
}
