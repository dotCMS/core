package org.apache.velocity.runtime;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@code velocimacro.library.fail-on-missing} flag added in issue #35601.
 *
 * <p>The dotCMS-patched {@link VelocimacroFactory} previously swallowed
 * {@link ResourceNotFoundException} when loading any configured {@code velocimacro.library}
 * file, allowing engine init to succeed while leaving global macros unregistered. After
 * pod restarts with transient I/O errors (notably EFS-backed K8s volumes),
 * {@code #renderMarks} and {@code #editContentlet} would render as literal text on every
 * page until the JVM was restarted.
 *
 * <p>The flag defaults to {@code true} — engine init now throws {@link VelocityException}.
 * Operators that intentionally configure optional libraries may opt out with
 * {@code velocimacro.library.fail-on-missing=false}.
 */
public class VelocimacroFactoryTest {

    private static final String MISSING_LIBRARY = "this-library-does-not-exist.vm";

    private RuntimeServices rsvc;

    @Before
    public void setUp() {
        rsvc = mock(RuntimeServices.class);
        when(rsvc.getProperty(RuntimeConstants.VM_LIBRARY)).thenReturn(MISSING_LIBRARY);
        when(rsvc.getTemplate(MISSING_LIBRARY))
                .thenThrow(new ResourceNotFoundException(
                        "Cannot find resource '" + MISSING_LIBRARY + "'"));
        // Permission flags read after the library loop — return defaults so init completes
        // when fail-on-missing=false.
        when(rsvc.getBoolean(anyString(), anyBoolean())).thenAnswer(invocation ->
                invocation.<Boolean>getArgument(1));
    }

    @Test
    public void initThrowsWhenFailOnMissingIsTrue() {
        when(rsvc.getBoolean(eq(RuntimeConstants.VM_LIBRARY_FAIL_ON_MISSING), anyBoolean()))
                .thenReturn(true);

        final VelocimacroFactory factory = new VelocimacroFactory(rsvc);
        try {
            factory.initVelocimacro();
            fail("Expected VelocityException when " + RuntimeConstants.VM_LIBRARY_FAIL_ON_MISSING
                    + "=true and a velocimacro.library file fails to load");
        } catch (VelocityException expected) {
            assertTrue(
                    "Exception message should name the failed library: " + expected.getMessage(),
                    expected.getMessage().contains(MISSING_LIBRARY));
        }
    }

    @Test
    public void initThrowsByDefault() {
        // Default value passed to getBoolean is true — the test setUp returns it as-is,
        // simulating the absence of an explicit property.
        final VelocimacroFactory factory = new VelocimacroFactory(rsvc);
        try {
            factory.initVelocimacro();
            fail("Expected VelocityException by default when a velocimacro.library file fails to load");
        } catch (VelocityException expected) {
            assertTrue(expected.getMessage().contains(MISSING_LIBRARY));
        }
    }

    @Test
    public void initSucceedsWhenFailOnMissingIsFalse() {
        when(rsvc.getBoolean(eq(RuntimeConstants.VM_LIBRARY_FAIL_ON_MISSING), anyBoolean()))
                .thenReturn(false);

        // Legacy silent-warn behavior — engine init completes even though the macro
        // library could not be loaded.
        final VelocimacroFactory factory = new VelocimacroFactory(rsvc);
        factory.initVelocimacro();
    }
}
