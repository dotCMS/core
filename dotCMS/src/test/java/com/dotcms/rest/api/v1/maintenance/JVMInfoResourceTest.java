package com.dotcms.rest.api.v1.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotmarketing.util.Config;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for how {@link JVMInfoResource} hides values in the environment, system property and configuration
 * override sections it returns.
 */
class JVMInfoResourceTest {

    private static final String MASK = "*****";

    /**
     * Method to test: {@link JVMInfoResource#obfuscateIfNeeded(String, Object)}.
     * Given scenario: a name that holds one of the words salt, pwd, credential, auth, authorization, jdbc, dsn or
     * bearer as a whole word or as a word of a camel-case name, with and without the {@code DOT_} prefix.
     * Expected result: the value is replaced by the mask.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"DOTCMS_CLUSTER_SALT", "DOT_DOTCMS_CLUSTER_SALT", "TRUSTSTORE_PWD", "DOT_TRUSTSTORE_PWD",
            "APP_CREDENTIALS", "SMTP_AUTH_USER", "DB_JDBC_URL", "dotcms.cluster.salt", "HTTP_AUTHORIZATION",
            "SENTRY_DSN", "API_BEARER", "spring.datasource.jdbcUrl", "dbPwd", "smtpAuth", "credentialsProvider"})
    void obfuscateIfNeeded_sensitiveName_isMasked(final String key) {
        assertEquals(MASK, JVMInfoResource.obfuscateIfNeeded(key, "sample-value"));
    }

    /**
     * Method to test: {@link JVMInfoResource#obfuscateIfNeeded(String, Object)}.
     * Given scenario: a key that the system table refuses to read or write, whose name holds none of the masked
     * words, as is, with the {@code DOT_} prefix in either case, and in property form.
     * Expected result: the value is replaced by the mask.
     */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"DOTCMS_CLUSTER_ID", "DOT_DOTCMS_CLUSTER_ID", "dot_DOTCMS_CLUSTER_ID", "dotcms.cluster.id"})
    void obfuscateIfNeeded_keyRefusedBySystemTable_isMasked(final String key) {
        assertEquals(MASK, JVMInfoResource.obfuscateIfNeeded(key, "sample-cluster-id"));
    }

    /**
     * Method to test: {@link JVMInfoResource#obfuscateIfNeeded(String, Object)}.
     * Given scenario: {@code SYSTEM_TABLE_BLACKLISTED_KEYS} names {@code my.private.value}, set after other keys were
     * already checked; the key is read in property form, as an environment variable, and inside a JVM option.
     * Expected result: the value is replaced by the mask in all three forms; without the property it is shown.
     */
    @Test
    void obfuscateIfNeeded_keyAddedToTheSystemTableBlacklist_isMasked() {
        assertEquals("shown", JVMInfoResource.obfuscateIfNeeded("my.private.value", "shown"));
        Config.setProperty("SYSTEM_TABLE_BLACKLISTED_KEYS", "my.private.value");
        try {
            assertEquals(MASK, JVMInfoResource.obfuscateIfNeeded("my.private.value", "hidden"));
            assertEquals(MASK, JVMInfoResource.obfuscateIfNeeded("DOT_MY_PRIVATE_VALUE", "hidden"));
            assertEquals("-Dmy.private.value=*****",
                    JVMInfoResource.obfuscateIfNeeded("JAVA_OPTS", "-Dmy.private.value=hidden"));
        } finally {
            Config.setProperty("SYSTEM_TABLE_BLACKLISTED_KEYS", null);
        }
        assertEquals("shown", JVMInfoResource.obfuscateIfNeeded("my.private.value", "shown"));
    }

    /**
     * Method to test: {@link JVMInfoResource#obfuscateIfNeeded(String, Object)}.
     * Given scenario: a masked name with a one-character value and with a longer value.
     * Expected result: the mask alone, with no character of the value.
     */
    @Test
    void obfuscateIfNeeded_maskedValue_keepsNoCharacterOfTheValue() {
        assertEquals(MASK, JVMInfoResource.obfuscateIfNeeded("DB_PASSWORD", "x"));
        assertEquals(MASK, JVMInfoResource.obfuscateIfNeeded("DB_PASSWORD", "abcdefgh-xyz"));
    }

    /**
     * Method to test: {@link JVMInfoResource#obfuscateIfNeeded(String, Object)}.
     * Given scenario: a name with no masked word whose value is a URL that carries a user name and a password.
     * Expected result: the user information of the URL is replaced by the mask and the rest of the URL is kept.
     */
    @Test
    void obfuscateIfNeeded_urlWithUserInfo_masksTheUserInfo() {
        assertEquals("jdbc:postgresql://*****@db:5432/dotcms",
                JVMInfoResource.obfuscateIfNeeded("DB_BASE_URL", "jdbc:postgresql://dbuser:s3cret@db:5432/dotcms"));
        assertEquals("redis://*****@cache:6379/0",
                JVMInfoResource.obfuscateIfNeeded("CACHE_URL", "redis://cacheuser:p@ss@cache:6379/0"));
    }

    /**
     * Method to test: {@link JVMInfoResource#obfuscateIfNeeded(String, Object)}.
     * Given scenario: a name with no masked word whose value holds {@code name=value} pairs, one of them with a
     * password name.
     * Expected result: only the value of that pair is replaced by the mask.
     */
    @Test
    void obfuscateIfNeeded_sensitivePairInValue_masksOnlyThatValue() {
        assertEquals("-Xmx1g -Djavax.net.ssl.trustStorePassword=***** -Dfoo=bar",
                JVMInfoResource.obfuscateIfNeeded("JAVA_OPTS",
                        "-Xmx1g -Djavax.net.ssl.trustStorePassword=abc123 -Dfoo=bar"));
    }

    /**
     * Method to test: {@link JVMInfoResource#obfuscateIfNeeded(String, Object)}.
     * Given scenario: JVM options whose name is one of the new words right after {@code -D} or {@code --}, a refused
     * key as a JVM option, quoted values with a space, an authorization scheme and its token, and pairs nested in the
     * value of another pair or inside a URL.
     * Expected result: each sensitive value is replaced by the mask and the rest of the text is kept.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("optionsAndNestedPairs")
    void obfuscateIfNeeded_optionsAndNestedPairs_maskOnlyTheirValues(final String value, final String expected) {
        assertEquals(expected, JVMInfoResource.obfuscateIfNeeded("JAVA_OPTS", value));
    }

    /**
     * Values for {@link #obfuscateIfNeeded_optionsAndNestedPairs_maskOnlyTheirValues(String, String)}: the value and
     * the expected result.
     */
    static Stream<Arguments> optionsAndNestedPairs() {
        return Stream.of(
                Arguments.of("-Dsalt=abc -Djdbc.url=x -Dauth=y --pwd=z -Dfoo=bar",
                        "-Dsalt=***** -Djdbc.url=***** -Dauth=***** --pwd=***** -Dfoo=bar"),
                Arguments.of("-DDOTCMS_CLUSTER_ID=abc -Dfoo=bar", "-DDOTCMS_CLUSTER_ID=***** -Dfoo=bar"),
                Arguments.of("password=\"my secret\" next", "password=***** next"),
                Arguments.of("password='my secret' next", "password=***** next"),
                Arguments.of("-Dauthorization=Basic abc123 next", "-Dauthorization=***** next"),
                Arguments.of("-Ddb.url=jdbc:postgresql://h/db?password=s3cret&user=u",
                        "-Ddb.url=jdbc:postgresql://h/db?password=*****&user=u"),
                Arguments.of("jdbc:sqlserver://h;user=u;password=p@ss", "jdbc:sqlserver://h;user=u;password=*****"),
                Arguments.of("-Dx=password=s3cret", "-Dx=password=*****"));
    }

    /**
     * Method to test: {@link JVMInfoResource#obfuscateIfNeeded(String, Object)}.
     * Given scenario: {@code SYSTEM_TABLE_BLACKLISTED_KEYS} holds a pattern that does not compile.
     * Expected result: no exception; every value is replaced by the mask until the property is fixed.
     */
    @Test
    void obfuscateIfNeeded_invalidSystemTableBlacklist_masksEveryValue() {
        Config.setProperty("SYSTEM_TABLE_BLACKLISTED_KEYS", "SAMPLE_[");
        try {
            assertEquals(MASK, JVMInfoResource.obfuscateIfNeeded("JAVA_HOME", "/opt/java"));
        } finally {
            Config.setProperty("SYSTEM_TABLE_BLACKLISTED_KEYS", null);
        }
        assertEquals("/opt/java", JVMInfoResource.obfuscateIfNeeded("JAVA_HOME", "/opt/java"));
    }

    /**
     * Method to test: {@link JVMInfoResource#obfuscateIfNeeded(String, Object)}.
     * Given scenario: a neutral name whose value is a 200,000-character run of letters, digits and dots, with and
     * without a {@code =} at the end.
     * Expected result: the scan returns the value well within two seconds.
     */
    @Test
    void obfuscateIfNeeded_longValue_isScannedInLinearTime() {
        final String run = "a1.".repeat(66_667);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            assertEquals(run, JVMInfoResource.obfuscateIfNeeded("BLOB", run));
            assertEquals(run + "=x", JVMInfoResource.obfuscateIfNeeded("BLOB", run + "=x"));
        });
    }

    /**
     * Method to test: {@link JVMInfoResource#obfuscateIfNeeded(String, Object)}.
     * Given scenario: names that hold no masked word, including names where auth or pwd is only part of a longer
     * word or the whole name of the shell's working directory, with plain values.
     * Expected result: the value is returned as it is.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "JAVA_HOME, /opt/java",
            "SITE_URL, https://example.com:8443/path?section=a",
            "DOT_AUTHORING_MODE, enabled",
            "AUTHOR_NAME, Jane",
            "authorName, Jane",
            "PWD, /srv",
            "OLDPWD, /home/dotcms",
            "PROFILE_URL, https://example.com/@team/page"})
    void obfuscateIfNeeded_neutralValue_isUnchanged(final String key, final String value) {
        assertEquals(value, JVMInfoResource.obfuscateIfNeeded(key, value));
    }

    /**
     * Method to test: {@link JVMInfoResource#obfuscateIfNeeded(String, Object)}.
     * Given scenario: no value and an empty value.
     * Expected result: an empty string, as before.
     */
    @Test
    void obfuscateIfNeeded_missingValue_isEmpty() {
        assertEquals("", JVMInfoResource.obfuscateIfNeeded("DB_PASSWORD", null));
        assertEquals("", JVMInfoResource.obfuscateIfNeeded("DB_PASSWORD", ""));
    }

    /**
     * Method to test: {@link JVMInfoResource#obfuscatePattern}, the configurable pattern that
     * {@code ConfigurationResource} also uses to leave keys out of the front-end configuration.
     * Given scenario: no {@code OBFUSCATE_SYSTEM_ENVIRONMENTAL_VARIABLES} property.
     * Expected result: the default still matches password names and does not match the words added to the fixed
     * pattern.
     */
    @Test
    void configurablePattern_default_isUnchanged() {
        assertTrue(JVMInfoResource.obfuscatePattern.matcher("DB_PASSWORD").find());
        assertFalse(JVMInfoResource.obfuscatePattern.matcher("DOTCMS_CLUSTER_SALT").find());
        assertFalse(JVMInfoResource.obfuscatePattern.matcher("AUTH_TYPE").find());
    }
}
