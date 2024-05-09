package com.dotcms.security.apps;

import static com.dotcms.security.apps.AppsUtil.bytesToCharArrayUTF;
import static com.dotcms.security.apps.AppsUtil.charsToBytesUTF;
import static com.dotcms.security.apps.AppsUtil.readJson;
import static com.dotcms.security.apps.AppsUtil.toJsonAsChars;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dotcms.rest.api.v1.apps.Input;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.liferay.util.Encryptor;
import com.liferay.util.EncryptorException;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class AppsUtilTest {

    private static final char[] VALUE = "value".toCharArray();
    private static final char[] OVERRIDDEN_VALUE = "overriddenValue".toCharArray();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        SecretsStore.INSTANCE.get().deleteAll();
    }

    /**
     * Data provider to feed byte conversion test.
     * @return
     * @throws Exception
     */
    @DataProvider
    public static Object[] getTestCases() {
        return new Object[]{
                new UTFCharsRangeTestCase(0, 127, "C0 Controls and Basic Latin"),
                new UTFCharsRangeTestCase(128, 255, "C1 Controls and Latin-1 Supplement"),
                new UTFCharsRangeTestCase(256, 383, "Latin Extended-A"),
                new UTFCharsRangeTestCase(384, 591, "Latin Extended-B"),
                new UTFCharsRangeTestCase(688, 767, "Spacing Modifiers"),
                new UTFCharsRangeTestCase(768, 879, "Diacritical Marks"),
                new UTFCharsRangeTestCase(880, 1023, "Greek and Coptic"),
                new UTFCharsRangeTestCase(1024, 1279, "Cyrillic Basic"),
                new UTFCharsRangeTestCase(1280, 1327, "Cyrillic Supplement"),
                new UTFCharsRangeTestCase(8192, 8303, "General Punctuation"),
                new UTFCharsRangeTestCase(8352, 8399, "Currency Symbols"),
                new UTFCharsRangeTestCase(8448, 8527, "Letterlike Symbols"),
                new UTFCharsRangeTestCase(8592, 8703, "Arrows"),
                new UTFCharsRangeTestCase(8704, 8959, "Mathematical Operators"),
                new UTFCharsRangeTestCase(9472, 9599, "Box Drawings"),
                new UTFCharsRangeTestCase(9600, 9631, "Block Elements"),
                new UTFCharsRangeTestCase(9632, 9727, "Geometric Shapes"),
                new UTFCharsRangeTestCase(9728, 9983, "Miscellaneous Symbols"),
                new UTFCharsRangeTestCase(9984, 10175, "Dingbats")
        };
    }

    /**
     * Tests the two internal methods used to transform a text stored as a array of chars in UTF-8
     * convert those into a byte array and back.
     * Given Scenario: a set of char codes representing a UTF-8 range then the set is turned into bytes and back
     * Expected Result: The original set of chars is restored after the byte array
     * @param testCase
     * @throws IOException
     */
    @Test
    @UseDataProvider("getTestCases")
    public void Test_BytesToChars_No_Middle_String_Conversion(final UTFCharsRangeTestCase testCase)
            throws IOException {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = testCase.fromCode; i <= testCase.toCode; i++) {
            final String string = fromCharCode(i);
            stringBuilder.append(string);
        }
        final String input = stringBuilder.toString();
        Logger.info(AppsAPIImplTest.class, () -> String
                .format(" UTF Charset code from `%d` to `%d`  %s `%s` ", testCase.fromCode,
                        testCase.toCode, testCase.description, input));
        final char[] chars = bytesToCharArrayUTF(input.getBytes(StandardCharsets.UTF_8));
        final byte[] bytes = charsToBytesUTF(chars);
        final String output = new String(bytes, StandardCharsets.UTF_8);
        assertEquals(input, output);
    }

    /**
     * https://www.w3schools.com/charsets/ref_html_utf8.asp
     *
     * @param codePoints char code see utf char codes.
     * @return the utf string representation.
     */
    private static String fromCharCode(final int... codePoints) {
        return new String(codePoints, 0, codePoints.length);
    }

    static class UTFCharsRangeTestCase {

        final int fromCode;
        final int toCode;
        final String description;

        UTFCharsRangeTestCase(final int fromCode, final int toCode, final String description) {
            this.fromCode = fromCode;
            this.toCode = toCode;
            this.description = description;
        }
    }

    /**
     * This tests the two methods used to serialize a secret converting it into a json stored as chars
     * then and putting it back together as an object.
     * Given Scenario: A Random Secret object is constructed with a random alphanumeric string then serialize to bytes and back.
     * Expected Result: The resulting object must match the original one passed in.
     * @throws DotDataException
     */
    @Test
    public void Test_Secret_Json_Serialization_No_String_Middle_Man() throws DotDataException {
        final AppSecrets secretsIn = new AppSecrets.Builder()
                .withKey("TheKey")
                .withHiddenSecret("hidden1", "I'm hidden")
                .withSecret("non-hidden1", "I'm not hidden")
                .withSecret("non-hidden5", RandomStringUtils.randomAlphanumeric(2337))
                .withSecret("bool1", true)
                .build();
        final char[] toJsonAsChars = toJsonAsChars(secretsIn);
        final AppSecrets secretsOut = readJson(toJsonAsChars);
        assertEquals(secretsIn.getKey(), secretsOut.getKey());

        assertEquals(secretsIn.getSecrets().size(), secretsOut.getSecrets().size());

        final Set<Entry<String, Secret>> secretsInEntries = secretsIn.getSecrets().entrySet();
        for (final Entry<String, Secret> entryIn : secretsInEntries) {
            assertEquals(secretsIn.getKey(), secretsOut.getKey());
            final Secret out = secretsOut.getSecrets().get(entryIn.getKey());
            assertNotNull(out);
            assertTrue(out.equals(entryIn.getValue()));//This does a deepEquals.
        }
    }

    /**
     * This basically test the encryption decryption round trip that a text could take.
     * Given Scenario:
     * Expected Result:
     * @throws EncryptorException
     */
    @Test
    public void Test_Encrypt_Decrypt_Text_No_Middle_String() throws EncryptorException {
        final Key key = Encryptor.generateKey();
        final String input = RandomStringUtils.randomAlphanumeric(1000);
        final char[] chars = AppsUtil.encrypt(key, input.toCharArray());
        final char[] decrypted = AppsUtil.decrypt(key, new String(chars));
        Assert.assertEquals(input, new String(decrypted));
    }

    /**
     * Keys have fixed values. (32 is the minimum valid length) A key can't have a random length.
     * Given scenario: We need to generate an encryption Key out of a given password.
     * Expected Result: Here we simply transform any password adding or removing fill-chars to fit a 32 length
     */
     @Test
     public void Test_Seed_Key_Add_Remove_Padding(){
         assertEquals(AppsUtil.keySeed(RandomStringUtils.randomAlphanumeric(14)).length(),32);
         assertEquals(AppsUtil.keySeed(RandomStringUtils.randomAlphanumeric(140)).length(),32);
         assertEquals(AppsUtil.keySeed(RandomStringUtils.randomAlphanumeric(32)).length(),32);
         assertEquals(AppsUtil.keySeed(RandomStringUtils.randomAlphanumeric(3)).length(),32);
         assertEquals(AppsUtil.keySeed(RandomStringUtils.randomAlphanumeric(0)).length(),32);
     }

    /**
     * Given a null {@link Input} object
     * Then method should return an empty optional.
     */
    @Test
    public void Test_dynamicSecret_empty() {
         final Optional<Secret> emptySecret = AppsUtil.dynamicSecret("key", "param", null);
         assertTrue(emptySecret.isEmpty());
    }

    /**
     * Given a non-null {@link Input} object
     * Then method should return a non-empty optional
     * And with the secret created based on the given input.
     */
    @Test
    public void Test_dynamicSecret() {
        final Input input = Input.newInputParam("value".toCharArray(), false);
        final Optional<Secret> secret = AppsUtil.dynamicSecret("key", "param", input);
        assertTrue(secret.isPresent());
        assertEquals("value", String.valueOf(secret.get().getValue()));
        assertFalse(secret.get().isHidden());
        assertSame(Type.STRING, secret.get().getType());
        assertNull(secret.get().getEnvVar());
        assertTrue(secret.get().isEnvShow());
        assertArrayEquals(StringUtils.BLANK_CHARS, secret.get().envVarValue);
    }

    /**
     * Given a null {@link ParamDescriptor} object
     * Then method should return an empty optional.
     */
    @Test
    public void Test_paramSecret_empty() {
        final Optional<Secret> emptySecret = AppsUtil.paramSecret("key", "param", VALUE, null);
        assertTrue(emptySecret.isEmpty());
    }

    /**
     * Given a non-null {@link ParamDescriptor} object
     * Then method should return a non-empty optional
     * And with the secret created based on the given param descriptor.
     */
    @Test
    public void Test_paramSecret() {
        final ParamDescriptor paramDescriptor = ParamDescriptor.builder()
                .withValue(VALUE)
                .withHidden(false)
                .withType(Type.STRING)
                .withEnvVar("envVar")
                .withEnvShow(true)
                .build();
        final Optional<Secret> secret = AppsUtil.paramSecret("key", "param", VALUE, paramDescriptor);
        assertTrue(secret.isPresent());
        assertArrayEquals(VALUE, secret.get().getValue());
        assertFalse(secret.get().isHidden());
        assertSame(Type.STRING, secret.get().getType());
        assertEquals("envVar", secret.get().getEnvVar());
        assertTrue(secret.get().isEnvShow());
        assertArrayEquals(StringUtils.BLANK_CHARS, secret.get().envVarValue);
    }

    /**
     * Given a non-null {@link ParamDescriptor} object
     * And environment variables are set
     * Then method should return a non-empty optional
     * And with the secret created based on the given param descriptor.
     */
    @Test
    public void Test_paramSecret_overrideWithEnvVar() {
        final ParamDescriptor paramDescriptor1 = ParamDescriptor.builder()
                .withHidden(true)
                .withType(Type.STRING)
                .withEnvVar("SOME_APP_ENV_VAR")
                .withEnvShow(true)
                .build();
        final Optional<Secret> secret1 = AppsUtil.paramSecret("key", "param", VALUE, paramDescriptor1);
        assertTrue(secret1.isPresent());
        assertArrayEquals(VALUE, secret1.get().getValue());
        assertTrue(secret1.get().isHidden());
        assertSame(Type.STRING, secret1.get().getType());
        assertEquals("SOME_APP_ENV_VAR", secret1.get().getEnvVar());
        assertTrue(secret1.get().isEnvShow());
        assertArrayEquals(OVERRIDDEN_VALUE, secret1.get().envVarValue);

        final ParamDescriptor paramDescriptor2 = ParamDescriptor.builder()
                .withHidden(true)
                .withType(Type.STRING)
                .withEnvShow(true)
                .build();
        final Optional<Secret> secret2 = AppsUtil.paramSecret("testApp", "testParam", VALUE, paramDescriptor2);
        assertTrue(secret2.isPresent());
        assertArrayEquals(VALUE, secret2.get().getValue());
        assertTrue(secret2.get().isHidden());
        assertSame(Type.STRING, secret2.get().getType());
        assertNull(secret2.get().getEnvVar());
        assertTrue(secret2.get().isEnvShow());
        assertArrayEquals(OVERRIDDEN_VALUE, secret2.get().envVarValue);
    }

    /**
     * Given a null {@link ParamDescriptor} object
     * And a null {@link AppSecrets} object
     * And providing an non-existing key
     * And providing a missing param descriptor
     * Then method should return an empty optional.
     */
    @Test
    public void Test_hiddenSecret_empty() {
        final Optional<Secret> emptySecret1 = AppsUtil.hiddenSecret("key", "param", null, null);
        assertTrue(emptySecret1.isEmpty());

        final Optional<Secret> emptySecret2 = AppsUtil.hiddenSecret("key", "param", null, AppSecrets.empty());
        assertTrue(emptySecret2.isEmpty());

        final Optional<Secret> emptySecret3 = AppsUtil.hiddenSecret(
                "key",
                "not-to-be-found",
                null,
                AppSecrets.builder()
                        .withKey("key")
                        .withHiddenSecret("param", "value")
                        .build());
        assertTrue(emptySecret3.isEmpty());

        final Optional<Secret> emptySecret4 = AppsUtil.hiddenSecret(
                "key",
                "param",
                null,
                AppSecrets.builder()
                        .withKey("key")
                        .withHiddenSecret("param", "value")
                        .build());
        assertTrue(emptySecret4.isEmpty());
    }

    /**
     * Given a non-null {@link ParamDescriptor} object
     * And a non-null {@link AppSecrets} object
     * Then method should return a non-empty optional
     * And with the secret created based on the given param descriptor and app secrets.
     */
    @Test
    public void Test_hiddenSecret() {
        final ParamDescriptor paramDescriptor = ParamDescriptor.builder()
                .withHidden(false)
                .withType(Type.STRING)
                .withEnvVar("envVar")
                .withEnvShow(true)
                .build();
        final Optional<Secret> secret = AppsUtil.hiddenSecret(
                "key",
                "param",
                paramDescriptor,
                AppSecrets.builder()
                        .withKey("key")
                        .withHiddenSecret("param", "value")
                        .build());
        assertTrue(secret.isPresent());
        assertArrayEquals(VALUE, secret.get().getValue());
        assertFalse(secret.get().isHidden());
        assertSame(Type.STRING, secret.get().getType());
        assertEquals("envVar", secret.get().getEnvVar());
        assertTrue(secret.get().isEnvShow());
        assertArrayEquals(StringUtils.BLANK_CHARS, secret.get().envVarValue);
    }

    /**
     * Given a non-null {@link ParamDescriptor} object
     * And a non-null {@link AppSecrets} object
     * And environment variables are set
     * Then method should return a non-empty optional
     * And with the secret created based on the given param descriptor and app secrets.
     */
    @Test
    public void Test_hiddenSecret_overrideWithEnVar() {
        final ParamDescriptor paramDescriptor1 = ParamDescriptor.builder()
                .withHidden(true)
                .withType(Type.STRING)
                .withEnvVar("SOME_APP_ENV_VAR")
                .withEnvShow(true)
                .build();
        final Optional<Secret> secret1 = AppsUtil.hiddenSecret(
                "key",
                "param",
                paramDescriptor1,
                AppSecrets.builder()
                        .withKey("key")
                        .withHiddenSecret("param", "value")
                        .build());
        assertTrue(secret1.isPresent());
        assertArrayEquals(VALUE, secret1.get().getValue());
        assertTrue(secret1.get().isHidden());
        assertSame(Type.STRING, secret1.get().getType());
        assertEquals("SOME_APP_ENV_VAR", secret1.get().getEnvVar());
        assertTrue(secret1.get().isEnvShow());
        assertArrayEquals(OVERRIDDEN_VALUE, secret1.get().envVarValue);

        final ParamDescriptor paramDescriptor2 = ParamDescriptor.builder()
                .withHidden(true)
                .withType(Type.STRING)
                .withEnvShow(true)
                .build();
        final Optional<Secret> secret2 = AppsUtil.hiddenSecret(
                "testApp",
                "testParam",
                paramDescriptor2,
                AppSecrets.builder()
                        .withKey("testApp")
                        .withHiddenSecret("testParam", "value")
                        .build());
        assertTrue(secret2.isPresent());
        assertArrayEquals(VALUE, secret2.get().getValue());
        assertTrue(secret2.get().isHidden());
        assertSame(Type.STRING, secret2.get().getType());
        assertNull(secret2.get().getEnvVar());
        assertTrue(secret2.get().isEnvShow());
        assertArrayEquals(OVERRIDDEN_VALUE, secret2.get().envVarValue);
    }

    /**
     * Given scenario: A secret is created with a key and a set of secrets
     * Expected Result: The secret is created exported and imported back.
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void testExportThenImportSecrets() throws DotDataException, IOException {

        final Key key = AppsUtil.generateKey(AppsUtil.loadPass(()->"lol"));

        final AppSecrets secrets = AppSecrets.builder()
                .withKey("key")
                .withHiddenSecret("hidden1", "value1")
                .withHiddenSecret("hidden2", "value2")
                .withSecret("secret1", "value3")
                .withSecret("secret2", Secret.builder().withType(Type.STRING).withValue("value4").withEnvValue("env".toCharArray()).build())
                .build();

        final Map<String, List<AppSecrets>> appSecrets = Map.of("SYSTEM_HOST", List.of(secrets));
        final AppsSecretsImportExport importExport = new AppsSecretsImportExport(appSecrets);
        final Path in = AppsUtil.exportSecret(importExport, key);

        final Map<String, List<AppSecrets>>  importSecrets = AppsUtil.importSecrets(in, key);
        assertNotNull(importSecrets);
        assertEquals(1, importSecrets.size());
        final List<AppSecrets> appSecretsList = importSecrets.get("SYSTEM_HOST");
        assertNotNull(appSecretsList);
        assertEquals(1, appSecretsList.size());
        final AppSecrets importedSecrets = appSecretsList.get(0);
        assertEquals("key", importedSecrets.getKey());
        assertEquals(4, importedSecrets.getSecrets().size());
        assertEquals("value1", new String(importedSecrets.getSecrets().get("hidden1").getValue()));
        assertEquals("value2", new String(importedSecrets.getSecrets().get("hidden2").getValue()));
        assertEquals("value3", new String(importedSecrets.getSecrets().get("secret1").getValue()));
        assertEquals("value4", new String(importedSecrets.getSecrets().get("secret2").getValue()));
        assertEquals("env", new String(importedSecrets.getSecrets().get("secret2").getEnvVarValue()));
    }

    /**
     * Given scenario: Lets import an old version of the secrets
     * Expected Result: The secret is imported back.
     * @throws DotDataException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testImportOldVersionSecrets()
            throws DotDataException, IOException, URISyntaxException {
        final URL resource = Thread.currentThread().getContextClassLoader()
                .getResource("apps/googleTranslationApp.export");
        assertNotNull("resource is null ", resource);
        final Path in = Path.of(resource.toURI());
        final Key key = AppsUtil.generateKey(AppsUtil.loadPass(()->"testdotcms1234"));
        final Map<String, List<AppSecrets>>  importSecrets = AppsUtil.importSecrets(in, key);
        assertNotNull(importSecrets);
        assertEquals(1, importSecrets.size());
        final List<AppSecrets> appSecretsList = importSecrets.get("SYSTEM_HOST");
        assertNotNull(appSecretsList);
        assertEquals(1, appSecretsList.size());
        final AppSecrets importedSecrets = appSecretsList.get(0);
        assertEquals("dotGoogleTranslate-config", importedSecrets.getKey());
        assertEquals(1, importedSecrets.getSecrets().size());
        assertEquals("testgoogleAPIAPP", new String(importedSecrets.getSecrets().get("apiKey").getValue()));

    }

}
