package com.dotcms.ai.workflow;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenAIAutoTagRunnerTest {

    /**
     * Given a contentlet whose identifier is null,
     * When the OpenAIAutoTagRunner constructor is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_constructor_missingIdentifier_throws() {
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getIdentifier()).thenReturn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> new OpenAIAutoTagRunner(contentlet, mock(User.class), true, false));
    }

    /**
     * Given a contentlet whose identifier is blank,
     * When the OpenAIAutoTagRunner constructor is called,
     * Then an IllegalArgumentException is thrown.
     */
    @Test
    public void test_constructor_emptyIdentifier_throws() {
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getIdentifier()).thenReturn("  ");

        assertThrows(
                IllegalArgumentException.class,
                () -> new OpenAIAutoTagRunner(contentlet, mock(User.class), true, false));
    }

    /**
     * Given a contentlet with a valid non-blank identifier,
     * When the OpenAIAutoTagRunner constructor is called,
     * Then the runner is created successfully without throwing.
     */
    @Test
    public void test_constructor_validIdentifier_succeeds() {
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getIdentifier()).thenReturn("abc-123");

        new OpenAIAutoTagRunner(contentlet, mock(User.class), true, false);
    }

}
