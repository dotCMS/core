package org.apache.velocity.runtime;

import com.dotmarketing.util.Config;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@code VELOCITY_LIBRARY_FAIL_ON_MISSING} dotCMS feature flag added in
 * issue #35601.
 *
 * <p>The dotCMS-patched {@link VelocimacroFactory} previously swallowed
 * {@link ResourceNotFoundException} when loading any configured {@code velocimacro.library}
 * file, allowing engine init to succeed while leaving global macros unregistered. After
 * pod restarts with transient I/O errors (notably EFS-backed K8s volumes),
 * {@code #renderMarks} and {@code #editContentlet} would render as literal text on every
 * page until the JVM was restarted.
 *
 * <p>The flag defaults to {@code false} — engine init preserves the legacy silent-warn
 * behavior so existing customers are not broken by this PR. Operators opt in with
 * {@code VELOCITY_LIBRARY_FAIL_ON_MISSING=true} (env var or system property) once they
 * have verified their library files load cleanly.
 */
public class VelocimacroFactoryTest {

    private static final String FLAG_KEY = "VELOCITY_LIBRARY_FAIL_ON_MISSING";
    private static final String MISSING_LIBRARY = "this-library-does-not-exist.vm";

    private RuntimeServices rsvc;
    private MockedStatic<Config> configMock;

    @Before
    public void setUp() {
        rsvc = mock(RuntimeServices.class);
        when(rsvc.getProperty(RuntimeConstants.VM_LIBRARY)).thenReturn(MISSING_LIBRARY);
        when(rsvc.getTemplate(MISSING_LIBRARY))
                .thenThrow(new ResourceNotFoundException(
                        "Cannot find resource '" + MISSING_LIBRARY + "'"));
        // Permission flags read after the library loop — return the supplied default
        // so init completes when the fail-on-missing flag is off.
        when(rsvc.getBoolean(anyString(), anyBoolean())).thenAnswer(invocation ->
                invocation.<Boolean>getArgument(1));
        configMock = mockStatic(Config.class);
        // Default behavior: return the supplied default for any flag we did not stub.
        configMock.when(() -> Config.getBooleanProperty(anyString(), anyBoolean()))
                .thenAnswer(invocation -> invocation.<Boolean>getArgument(1));
    }

    @After
    public void tearDown() {
        configMock.close();
    }

    @Test
    public void initThrowsWhenFlagIsTrue() {
        configMock.when(() -> Config.getBooleanProperty(eq(FLAG_KEY), anyBoolean()))
                .thenReturn(true);

        final VelocimacroFactory factory = new VelocimacroFactory(rsvc);
        try {
            factory.initVelocimacro();
            fail("Expected VelocityException when " + FLAG_KEY
                    + "=true and a velocimacro.library file fails to load");
        } catch (VelocityException expected) {
            assertTrue(
                    "Exception message should name the failed library: " + expected.getMessage(),
                    expected.getMessage().contains(MISSING_LIBRARY));
        }
    }

    @Test
    public void initDoesNotThrowByDefault() {
        // Default behavior — flag is off, legacy silent-warn path preserved.
        // The flag default (false) flows through the configMock's default thenAnswer.
        final VelocimacroFactory factory = new VelocimacroFactory(rsvc);
        factory.initVelocimacro();

        // Verify the library-loading branch actually ran — guards against future
        // refactors that might short-circuit before the load loop is reached.
        verify(rsvc).getTemplate(MISSING_LIBRARY);
    }

    @Test
    public void initDoesNotThrowWhenFlagIsExplicitlyFalse() {
        configMock.when(() -> Config.getBooleanProperty(eq(FLAG_KEY), anyBoolean()))
                .thenReturn(false);

        final VelocimacroFactory factory = new VelocimacroFactory(rsvc);
        factory.initVelocimacro();

        verify(rsvc).getTemplate(MISSING_LIBRARY);
    }
}
