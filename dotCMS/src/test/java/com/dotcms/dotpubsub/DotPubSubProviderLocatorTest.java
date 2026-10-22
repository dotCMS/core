package com.dotcms.dotpubsub;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.exception.DotRuntimeException;
import org.junit.Test;

/**
 * Exercises {@link DotPubSubProviderLocator}'s override resolution.
 * <p>
 * The behaviour under test is that a stale or otherwise unusable
 * {@code DOT_PUBSUB_PROVIDER_OVERRIDE} degrades to the default provider instead of preventing the
 * node from starting. {@link NullDotPubSubProvider} stands in for the default so these tests stay
 * free of database and API-locator infrastructure.
 */
public class DotPubSubProviderLocatorTest {

    private static final String DEFAULT = NullDotPubSubProvider.class.getCanonicalName();
    private static final String MISSING = "com.dotcms.dotpubsub.NoSuchProviderImpl";

    @Test
    public void resolveProvider_uses_the_requested_class_when_it_is_valid() {

        final DotPubSubProvider provider =
                DotPubSubProviderLocator.resolveProvider(DEFAULT, DEFAULT);

        assertTrue(provider instanceof NullDotPubSubProvider);
    }

    @Test
    public void resolveProvider_falls_back_when_the_class_is_missing() {

        final DotPubSubProvider provider =
                DotPubSubProviderLocator.resolveProvider(MISSING, DEFAULT);

        assertTrue(provider instanceof NullDotPubSubProvider);
    }

    @Test
    public void resolveProvider_falls_back_when_the_class_is_not_a_provider() {

        final DotPubSubProvider provider =
                DotPubSubProviderLocator.resolveProvider(String.class.getName(), DEFAULT);

        assertTrue(provider instanceof NullDotPubSubProvider);
    }

    @Test
    public void resolveProvider_falls_back_when_there_is_no_no_arg_constructor() {

        final DotPubSubProvider provider = DotPubSubProviderLocator
                .resolveProvider(NoNoArgConstructorProvider.class.getName(), DEFAULT);

        assertTrue(provider instanceof NullDotPubSubProvider);
    }

    @Test
    public void resolveProvider_falls_back_when_the_constructor_throws() {

        final DotPubSubProvider provider = DotPubSubProviderLocator
                .resolveProvider(FailingConstructorProvider.class.getName(), DEFAULT);

        assertTrue(provider instanceof NullDotPubSubProvider);
    }

    @Test
    public void resolveProvider_fails_when_the_fallback_is_itself_unusable() {

        assertThrows(DotRuntimeException.class,
                () -> DotPubSubProviderLocator.resolveProvider(MISSING, MISSING));
    }

    /** A provider with no no-arg constructor, so it cannot be built reflectively. */
    static class NoNoArgConstructorProvider extends NullDotPubSubProvider {

        NoNoArgConstructorProvider(final String requiredArgument) {
        }
    }

    /** A provider whose constructor always fails. */
    static class FailingConstructorProvider extends NullDotPubSubProvider {

        FailingConstructorProvider() {
            throw new IllegalStateException("constructor intentionally fails");
        }
    }

}
