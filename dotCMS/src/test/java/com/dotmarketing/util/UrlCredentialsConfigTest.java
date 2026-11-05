package com.dotmarketing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.BasicAuthentication;
import com.dotcms.config.SystemTableConfigSource;
import com.dotcms.featureflag.FeatureFlagName;
import java.lang.reflect.Field;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** The URL-login opt-in must fail closed at the winning configuration source, not fall back. */
public class UrlCredentialsConfigTest extends UnitTestBase {

    private static final String KEY = FeatureFlagName.ALLOW_USERNAME_PASSWORD_AS_URL_PARAMS;
    private static final String ENV_KEY = Config.envKey(KEY);
    private Object previousProperty;
    private Object previousEnvProperty;
    private Object previousSource;
    private boolean previousSourceEnabled;
    private boolean previousEnvKeyPresent;
    private Field sourceField;
    private Set<String> environmentKeys;
    private SystemTableConfigSource source;

    /** Isolates the real Config resolution with a mocked system table and controlled environment key. */
    @Before
    @SuppressWarnings("unchecked")
    public void preserveConfiguration() throws ReflectiveOperationException {
        previousProperty = Config.props.getProperty(KEY);
        previousEnvProperty = Config.props.getProperty(ENV_KEY);
        sourceField = Config.class.getDeclaredField("systemTableConfigSource");
        sourceField.setAccessible(true);
        previousSource = sourceField.get(null);
        previousSourceEnabled = Config.enableSystemTableConfigSource;
        final Field environmentField = Config.class.getDeclaredField("environmentSetKeys");
        environmentField.setAccessible(true);
        environmentKeys = (Set<String>) environmentField.get(null);
        previousEnvKeyPresent = environmentKeys.contains(ENV_KEY);
        environmentKeys.remove(ENV_KEY);
        Config.props.clearProperty(ENV_KEY);
        source = mock(SystemTableConfigSource.class);
        sourceField.set(null, source);
        Config.enableSystemTableConfigSource = true;
    }

    /** Restores the exact property sources and environment precedence after every test. */
    @After
    public void restoreConfiguration() throws ReflectiveOperationException {
        sourceField.set(null, previousSource);
        Config.enableSystemTableConfigSource = previousSourceEnabled;
        Config.props.setProperty(KEY, previousProperty);
        Config.props.setProperty(ENV_KEY, previousEnvProperty);
        if (previousEnvKeyPresent) {
            environmentKeys.add(ENV_KEY);
        } else {
            environmentKeys.remove(ENV_KEY);
        }
    }

    /** A malformed environment override must not fall back to either enabled lower-priority source. */
    @Test
    public void invalidEnvironmentOverrideNeverFallsBackToEnabledFileOrSystemTable() {
        Config.props.setProperty(KEY, "true");
        when(source.getValue(KEY)).thenReturn("true");
        environmentKeys.add(ENV_KEY);
        for (final String value : new String[]{"invalid", "true,", "true,,", "true,false", "",
                "false", "true", "TRUE", " true "}) {
            Config.props.setProperty(ENV_KEY, value);
            assertEquals("Environment value: " + value, Boolean.parseBoolean(value.trim()),
                    BasicAuthentication.allowUrlCredentials());
        }
    }

    /** A cluster-file-only true must not merge with a shipped false into an ambiguous list. */
    @Test
    public void singleFileOptInDoesNotConflictWithTheShippedDefault() throws Exception {
        final Properties bundled = new Properties();
        try (InputStream input = Config.class.getResourceAsStream("/dotmarketing-config.properties")) {
            assertNotNull(input);
            bundled.load(input);
        }
        assertNull("The default belongs in the strict getter, not an additional file value",
                bundled.getProperty(KEY));
        Config.props.clearProperty(KEY);
        assertFalse(BasicAuthentication.allowUrlCredentials());
        Config.props.addProperty(KEY, "true");
        assertTrue(BasicAuthentication.allowUrlCredentials());
    }

    /** The default applies only to an absent value, never to an explicit empty or false value. */
    @Test
    public void defaultsApplyOnlyWhenThePropertyIsAbsent() {
        Config.props.clearProperty(KEY);
        assertTrue(Config.getStrictBooleanProperty(KEY, true));
        assertFalse(Config.getStrictBooleanProperty(KEY, false));
        Config.props.setProperty(KEY, "");
        assertFalse(Config.getStrictBooleanProperty(KEY, true));
        Config.props.setProperty(KEY, "false");
        assertFalse(Config.getStrictBooleanProperty(KEY, true));
    }

    /** Lists, empty fields and non-boolean properties must never be interpreted as a true scalar. */
    @Test
    public void propertyValuesRequireAnUnambiguousTrueScalar() {
        for (final String value : new String[]{"invalid", "true,", "true,,", "true,false", "",
                "false", "true", "TRUE", " true "}) {
            Config.props.setProperty(KEY, value);
            assertEquals("Property value: " + value, Boolean.parseBoolean(value.trim()),
                    BasicAuthentication.allowUrlCredentials());
        }
    }
}
