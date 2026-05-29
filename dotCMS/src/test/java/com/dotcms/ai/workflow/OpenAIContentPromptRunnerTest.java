package com.dotcms.ai.workflow;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenAIContentPromptRunnerTest {

    @Test
    public void test_constructor_missingIdentifier_throws() {
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getIdentifier()).thenReturn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> new OpenAIContentPromptRunner(contentlet, mock(User.class), "prompt", true, "field"));
    }

    @Test
    public void test_constructor_emptyIdentifier_throws() {
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getIdentifier()).thenReturn("  ");

        assertThrows(
                IllegalArgumentException.class,
                () -> new OpenAIContentPromptRunner(contentlet, mock(User.class), "prompt", true, "field"));
    }

    @Test
    public void test_constructor_validIdentifier_succeeds() {
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getIdentifier()).thenReturn("abc-123");

        new OpenAIContentPromptRunner(contentlet, mock(User.class), "prompt", true, "field");
    }

}
