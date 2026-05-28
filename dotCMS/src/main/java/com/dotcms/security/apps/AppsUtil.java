package com.dotcms.security.apps;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.apps.Input;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.io.ByteStreams;
import com.liferay.util.Base64;
import com.liferay.util.Encryptor;
import com.liferay.util.EncryptorException;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.commons.io.input.CharSequenceInputStream;
import org.apache.commons.lang3.ArrayUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.dotcms.security.apps.AppsAPI.DOT_GLOBAL_SERVICE;
import static com.dotcms.security.apps.AppsAPI.HOST_SECRET_KEY_SEPARATOR;
import static com.dotmarketing.util.UtilMethods.isNotSet;
import static com.dotmarketing.util.UtilMethods.isSet;

/**
 * This class provides useful methods to interact with information related to Apps in dotCMS.
 *
 * @author Fabrizzio Araya
 * @since Apr 15th, 2020
 */
public class AppsUtil {

    private static final String APP_PARAM_ENV_VAR_TEMPLATE = "APP_%s_PARAM_%s";
    private static final ObjectMapper mapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
    public static final String APPS_IMPORT_EXPORT_DEFAULT_PASSWORD = "APPS_IMPORT_EXPORT_DEFAULT_PASSWORD";

    /**
     * Segment used to address the System Host (global) form of an env var. The global tier is not a
     * special hostless name: it is the System Host expressed through the same
     * {@code DOT_{APP_KEY}_{HOSTNAME}_{APP_VALUE_KEY}} convention, e.g.
     * {@code DOT_{APP_KEY}_SYSTEM_HOST_{APP_VALUE_KEY}}.
     */
    public static final String SYSTEM_HOST_ENV_SEGMENT = "SYSTEM_HOST";

    /**
     * Tracks which legacy {@code APP_..._PARAM_...} env vars have already logged a deprecation
     * warning, so the warning fires at most once per JVM lifetime per app-key + value-key pair
     * (getSecrets can run on every request and would otherwise flood the logs).
     */
    private static final Set<String> LEGACY_ENV_DEPRECATION_LOGGED = ConcurrentHashMap.newKeySet();

    private AppsUtil() {
    }

    /**
     * One single method takes care of building the internal-key
     */
    public static String internalKey(final String serviceKey, final Host host) {
        return internalKey(serviceKey, host == null ? null : host.getIdentifier());
    }

    /**
     * Given a service key and an identifier this builds an internal key composed by the two values concatenated
     * And lowercased.
     * Like `5e096068-edce-4a7d-afb1-95f30a4fa80e:serviceKeyNameXYZ`
     * @param serviceKey
     * @param hostIdentifier
     * @return
     */
    public static String internalKey(final String serviceKey, final String hostIdentifier) {
        // if Empty ServiceKey is passed everything will be set under systemHostIdentifier:dotCMSGlobalService
        //Otherwise the internal Key will look like:
        // `5e096068-edce-4a7d-afb1-95f30a4fa80e:serviceKeyNameXYZ` where the first portion is the hostId
        final String key = isSet(serviceKey) ? serviceKey : DOT_GLOBAL_SERVICE;
        final String identifier =
                (null == hostIdentifier) ? APILocator.systemHost().getIdentifier() : hostIdentifier;
        return (identifier + HOST_SECRET_KEY_SEPARATOR + key).toLowerCase();
    }

    /**
     * Given the AppSecrets this will return a char array representing the deserialized json object.
     * No strings are created in the transformation process.
     * @param object
     * @return
     * @throws DotDataException
     */
    public static char[] toJsonAsChars(final AppSecrets object) throws DotDataException {
        try {
            final byte [] bytes = mapper.writeValueAsBytes(object);
            return bytesToCharArrayUTF(bytes);
        } catch (IOException e) {
            throw new DotDataException(e);
        }
    }

    /**
     * Takes a char array representation of a json secret and generates the domain object.
     * No strings are created in the transformation process.
     * @param chars
     * @return The Secrets domain model.
     * @throws DotDataException
     */
    static AppSecrets readJson(final char[] chars) throws DotDataException {
        return readJson(chars, true);
    }

    /**
     * Deserializes a stored secrets blob. When {@code applyLegacyEnvOverlay} is {@code true} the
     * legacy {@code APP_..._PARAM_...} env overlay is applied to each secret (historical override
     * behavior, retained for existing callers). When {@code false} the raw stored values are
     * returned without any env overlay — used by the tier-aware resolution path in
     * {@code AppsAPIImpl.getSecrets}, which applies the env tiers explicitly at the correct
     * precedence (host-stored beats legacy env).
     *
     * @param chars                 the char-array JSON representation of the stored blob
     * @param applyLegacyEnvOverlay whether to overlay the legacy env values onto the secrets
     * @return the deserialized {@link AppSecrets}
     */
    static AppSecrets readJson(final char[] chars, final boolean applyLegacyEnvOverlay)
            throws DotDataException {
        try {
            final byte [] bytes = charsToBytesUTF(chars);
            final AppSecrets appSecrets = mapper.readValue(bytes, AppSecrets.class);
            if (applyLegacyEnvOverlay) {
                appSecrets.getSecrets().entrySet().forEach(secret -> updateEnvValue(secret, appSecrets));
            }
            return appSecrets;
        } catch (IOException e) {
            throw new DotDataException(e);
        }
    }

    /**
     * Given a map of secrets this will update the envVarValue of each secret.
     *
     * @param secret secret entry
     * @param appSecrets app secrets to iterate against
     */
    private static void updateEnvValue(final Entry<String, Secret> secret, final AppSecrets appSecrets) {
        secret.getValue().setEnvVarValue(
                Optional.ofNullable(
                        discoverEnvVarValue(
                                () -> guessEnvVar(
                                        appSecrets.getKey(),
                                        secret.getKey()),
                                secret.getValue().getEnvVar()))
                        .map(String::toCharArray)
                        .orElse(null));
    }

    /**
     * This method takes a byte array and converts its contents into a char array
     * No String middle man is created.
     * https://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords
     * @param bytes
     * @return char array
     */
    static char[] bytesToCharArrayUTF(final byte[] bytes) throws IOException {
        final List<Integer> integers = new ArrayList<>(bytes.length);
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {

            int chr;
            while ((chr = reader.read()) != -1) {
                integers.add(chr);
            }
        }
        return ArrayUtils.toPrimitive(
                integers.stream().map(value -> (char) value.intValue()).toArray(Character[]::new));
    }

    /**
     * This method takes a char array and converts its contents into a byte array No String middle
     * man is created. https://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords
     * @param chars input
     * @return byte array
     */
    static byte[] charsToBytesUTF(final char[] chars) throws IOException {
        final CharSequence sequence = java.nio.CharBuffer.wrap(chars);
        return ByteStreams
                .toByteArray(new CharSequenceInputStream(sequence, StandardCharsets.UTF_8));
    }


    /**
     * Encrypt variant of the function of the same name located in The Encryptor util class.
     * The main difference is that this ones does not use a string in the middle. It directly takes a char array.
     * And doesn't use a string internally.
     * @see Encryptor#encrypt(Key, String)
     * @param key security Key
     * @param chars
     * @return encrypted text as a char array
     * @throws EncryptorException
     */
    static char[] encrypt(final Key key, final char[] chars)
            throws EncryptorException {
        try {
            final byte[] decryptedBytes = charsToBytesUTF(chars);
            final byte[] encryptedBytes = encrypt(key,decryptedBytes);
            final String encryptedString = Base64.encode(encryptedBytes);
            return encryptedString.toCharArray();
        } catch (Exception e) {
            throw new EncryptorException(e);
        }
    }

    /**
     * byte array encrypt function
     * @param key
     * @param decryptedBytes
     * @return
     * @throws EncryptorException
     */
    static byte[] encrypt(final Key key, final byte[] decryptedBytes)
            throws EncryptorException {
        try {
            final Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(decryptedBytes);
        } catch (Exception e) {
            throw new EncryptorException(e);
        }
    }

    /**
     * Decrypt variant of the function of the same name located in The Encryptor util class.
     * The main difference is that this ones does not use a string in the middle to extract the resulting bytes.
     * @see Encryptor#decrypt(Key, String)
     * @param key security Key
     * @param encryptedString encrypted string
     * @return decrypted text as a char array
     * @throws EncryptorException
     */
    static char[] decrypt(final Key key, final String encryptedString)
            throws EncryptorException {
        try {
            final byte[] encryptedBytes = Base64.decode(encryptedString);
            final Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key);
            final byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return bytesToCharArrayUTF(decryptedBytes);

        } catch (Exception e) {
            throw new EncryptorException(e);
        }
    }

    /**
     * byte array decrypt function
     * @param key
     * @param encryptedBytes
     * @return
     * @throws EncryptorException
     */
    static byte[] decrypt(final Key key, final byte [] encryptedBytes)throws EncryptorException {
        final Cipher cipher;
        try {
            cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(encryptedBytes);
        } catch (Exception e) {
            throw new EncryptorException(e);
        }
    }

    /**
     * Since we're generating a /key out of a given password
     * We need to adjust the size of the password by adding padding chars
     * The generated see will be 32 ascii chars long
     * @param password
     * @return
     */
    static String keySeed(final String password) {
        final int maxSize = 32;
        String keySeed = new String(password.getBytes(), StandardCharsets.US_ASCII);
        if (keySeed.length() > maxSize) {
            keySeed = keySeed.substring(0, maxSize);
        } else {
            final int count = Math.abs(maxSize - keySeed.length());
            final StringBuilder builder = new StringBuilder(keySeed);
            if (count > 0) {
                for (int i = 1; i <= count; i++) {
                    builder.append((char)i);
                }
            }
            keySeed = builder.toString();
        }
        return keySeed;
    }

    /**
     * given a seed password this will generate a Security Key
     * @param password
     * @return
     */
    public static Key generateKey(final String password) throws DotDataException {
        if(null == password){
            throw new IllegalArgumentException("No password has been provided. ");
        }
        final String seed = keySeed(password);
        final byte [] bytes = seed.getBytes();
        return new SecretKeySpec(bytes, 0, bytes.length, Encryptor.KEY_ALGORITHM);
    }

    /**
     * This method serves as a bridge to Encryptor{@link #digest(String)}
     * @param text
     * @return
     */
    public static String digest(final String text) {
        return Encryptor.digest(text);
    }


    /**
     * Loads the default password stored in the properties
     */
    public static String loadPass(final Supplier<String> passwordOverride) {
        String password = null;
        if (null != passwordOverride) {
            Logger.info(AppsUtil.class,"Apps Password Override supplier has been provided.");
            password = passwordOverride.get();
        }
        if(isNotSet(password)) {
            Logger.info(AppsUtil.class,"Apps Password default will be used.");
            password = Config.getStringProperty(APPS_IMPORT_EXPORT_DEFAULT_PASSWORD);
        }
        return password;
    }

    /**
     * Ths will return a list of AppSecrets arranged in a map whose key is the site-id
     * Therefore we have a Secrets by site
     * @param incomingFile
     * @param key
     * @return
     * @throws DotDataException
     * @throws IOException
     * @throws EncryptorException
     */

     public static Map<String, List<AppSecrets>> importSecrets(final Path incomingFile, final Key key)
            throws DotDataException, IOException {

        final byte[] encryptedBytes = Files.readAllBytes(incomingFile);
        final byte[] decryptedBytes;
        try {
            decryptedBytes = AppsUtil.decrypt(key, encryptedBytes);
        }catch (EncryptorException e){
            throw new IllegalArgumentException("An error occurred while decrypting file contents. ",e.getCause());
        }
        final File importFile = File.createTempFile("secrets", "export");
        try (OutputStream outputStream = Files.newOutputStream(importFile.toPath())) {
            outputStream.write(decryptedBytes);
        }
        final AppsSecretsImportExport importExport;
        try {
            importExport = readObject(importFile.toPath());
            return importExport.getSecrets();
        } catch (ClassNotFoundException e) {
            throw new DotDataException(e);
        }
    }

    /**
     * Reads the exported file stream
     * and returns a wrapper that contains all entries.
     * @param importFile
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static AppsSecretsImportExport readObject(final Path importFile)
            throws IOException, ClassNotFoundException {
        try(InputStream inputStream = Files.newInputStream(importFile)){
            return (AppsSecretsImportExport) new VersionOverrideObjectInputStream(inputStream).readObject();
        }
    }

    /**
     *
     * @param exportedSecrets
     * @param key
     * @return
     * @throws IOException
     * @throws DotDataException
     */
    static Path exportSecret(final AppsSecretsImportExport exportedSecrets,final Key key)
            throws IOException, DotDataException {
        final File tempFile = File.createTempFile("secretsExport", ".tmp");
        try {
            writeObject(exportedSecrets, tempFile.toPath());
            final byte[] bytes = Files.readAllBytes(tempFile.toPath());
            try {
                final File file = File.createTempFile("secrets", ".export");
                file.deleteOnExit();
                final byte[] encrypted = AppsUtil.encrypt(key, bytes);
                final Path path = file.toPath();
                try (OutputStream outputStream = Files.newOutputStream(path)) {
                    outputStream.write(encrypted);
                    return path;
                }
            } catch (EncryptorException e) {
                throw new DotDataException(e);
            }
        } finally {
            tempFile.delete();
        }
    }

    /**
     * Takes a wrapping object that encapsulates all entries an write'em out ino a stream
     * @param bean
     * @param file
     * @throws IOException
     */
    private static void writeObject(final AppsSecretsImportExport bean, final Path file)
            throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(file)) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
                objectOutputStream.writeObject(bean);
            }
        }
    }

    /**
     * Map of optionals. Common portable format
     */
    public static Map<String, Optional<char[]>> mapForValidation(final AppSecrets appSecrets) {
        return appSecrets.getSecrets().entrySet().stream()
                .collect(Collectors
                        .toMap(Entry::getKey,
                                secretEntry -> {
                                    final Secret value = secretEntry.getValue();
                                    return value == null ? Optional.empty()
                                            : Optional.of(value.getValue());
                                })
                );
    }

    /**
     * Validate the incoming params and match them with the params described by the respective appDescriptor yml.
     * This method takes a Map of Optional<char[]> As this is a middle ground object representation
     * that can be mapped from a saved  AppSecrets or an incoming SecretForm
     * if the param isn't included in the map it means it wasn't sent.
     * if the param was sent empty that would be represented as an empty optional.
     * I'm using optional since null values on map triggers warnings
     *
     * @param params
     * @param appDescriptor
     * @param appSecretsOptional
     */
    public static void validateForSave(final Map<String, Optional<char[]>> params,
                                       final AppDescriptor appDescriptor,
                                       final Optional<AppSecrets> appSecretsOptional) {

        //Param/Property names are case-sensitive.
        final Map<String, ParamDescriptor> appDescriptorParams = appDescriptor.getParams();

        for (final Entry<String, ParamDescriptor> descriptorParam : appDescriptorParams.entrySet()) {
            final String describedParamName = descriptorParam.getKey();
            //initialize to null so it is not found in the params map it means it wasn't sent.
            char[] input = null;
            if (params.containsKey(describedParamName)) {
                // if the key is found then verify if there's an actual value or else null
                final Optional<char[]> optionalChars = params.get(describedParamName);
                input = optionalChars.orElse(null);
            }

            if (isRequired(descriptorParam, appSecretsOptional, describedParamName)
                    && (input == null || isNotSet(input))) {
                throw new IllegalArgumentException(
                        String.format(
                                "Param `%s` is marked required in the descriptor but does not come with a value.",
                                describedParamName
                        )
                );
            }

            if (null == input) {
                //Param wasn't sent but it doesn't matter since it isn't required.
                Logger.debug(AppsAPIImpl.class, () -> String
                        .format("Non required param `%s` was set.",
                                describedParamName));
                continue;
            }

            if (Type.BOOL.equals(descriptorParam.getValue().getType()) && UtilMethods
                    .isSet(input)) {
                final String asString = new String(input);
                final boolean bool = (asString.equalsIgnoreCase(Boolean.TRUE.toString())
                        || asString.equalsIgnoreCase(Boolean.FALSE.toString()));
                if (!bool) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Can not convert value `%s` to type BOOL for param `%s`.",
                                    asString, describedParamName
                            )
                    );
                }
            }

            if (Type.SELECT.equals(descriptorParam.getValue().getType()) && UtilMethods
                    .isSet(input)) {
                final List<Map> list = descriptorParam.getValue().getList();
                final Set<String> values = list.stream().filter(map -> null != map.get("value"))
                        .map(map -> map.get("value").toString()).collect(Collectors.toSet());
                final String asString = new String(input);
                if (!values.contains(asString)) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Can not find value `%s` in the list of permitted values `%s`.",
                                    asString, describedParamName
                            )
                    );
                }
            }
        }

        if (!appDescriptor.isAllowExtraParameters()) {
            final SetView<String> extraParamsFound = Sets
                    .difference(params.keySet(), appDescriptorParams.keySet());

            if (!extraParamsFound.isEmpty()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Unknown additional params `%s` not allowed by the app descriptor.",
                                String.join(", ", extraParamsFound)
                        )
                );
            }
        }
    }

    /**
     * Returns the Secrets of a specific App for a given Site in dotCMS.
     *
     * @param appConfigId          The ID of the dotCMS App.
     * @param site                 The {@link Host} object whose Secrets will be retrieved.
     * @param fallbackOnSystemHost If {@code true}, this method will try to retrieve the Secrets from the System Host
     *                             if they are not found in the given Site.
     *
     * @return An Optional with the {@link AppSecrets} object.
     */
    public static Optional<AppSecrets> getAppSecrets(final String appConfigId, final Host site, final boolean fallbackOnSystemHost) {
        final Optional<AppSecrets> appSecrets = Try.of(() -> APILocator.getAppsAPI().getSecrets(appConfigId, fallbackOnSystemHost, site, APILocator.systemUser())).getOrNull();
        if (appSecrets.isEmpty()) {
            Logger.warn(AppsUtil.class, String.format("App ID '%s' for Site '%s' was not found", appConfigId, site));
        }
        return appSecrets;
    }

    /**
     * Creates a dynamic secret based on a given {@link Input}.
     * It will try to resolve an env-var value and set it in the secret.
     *
     * @param key the key of the App
     * @param paramName the name of the parameter
     * @param inputParam the input parameter
     * @return an Optional with the Secret object
     */
    public static Optional<Secret> dynamicSecret(final String key,
                                                 final String paramName,
                                                 final Input inputParam) {
        return Optional
                .ofNullable(inputParam)
                .map(dp -> Secret.builder()
                        .withValue(inputParam.getValue())
                        .withHidden(inputParam.isHidden())
                        .withType(Type.STRING)
                        .withEnvVar(null)
                        .withEnvShow(true)
                        .withEnvValue(discoverEnvVarValue(key, paramName, null))
                        .build());
    }

    /**
     * Creates a non-dynamic secret based on a given {@link ParamDescriptor}.
     * It will try to resolve an env-var value and set it in the secret.
     *
     * @param key the key of the App
     * @param paramName the name of the parameter
     * @param value the value of the parameter
     * @param paramDescriptor the parameter descriptor
     * @return an Optional with the Secret object
     */
    public static Optional<Secret> paramSecret(final String key,
                                               final String paramName,
                                               final char[] value,
                                               final ParamDescriptor paramDescriptor) {
        return Optional
                .ofNullable(paramDescriptor)
                .map(dp -> secretFromDescribedParam(key, paramName, value, paramDescriptor));
    }

    /**
     * Creates a non-dynamic hidden secret based on a given {@link ParamDescriptor} and the actual previously
     * resolved  {@link AppSecrets}.
     * It will try to resolve an env-var value and set it in the secret.
     *
     * @param key the key of the App
     * @param paramName the name of the parameter
     * @param describedParam the parameter descriptor
     * @param appSecrets the actual resolved AppSecrets
     * @return an Optional with the Secret object
     */
    public static Optional<Secret> hiddenSecret(final String key,
                                                final String paramName,
                                                final ParamDescriptor describedParam,
                                                final AppSecrets appSecrets) {
        return Optional
                .ofNullable(appSecrets)
                .map(AppSecrets::getSecrets)
                .map(secrets -> secrets.get(paramName))
                .map(secret -> secretFromDescribedParam(key, paramName, secret.getValue(), describedParam));
    }

    /**
     * Discovers the environment variable value based on the given application and parameter names.
     *
     * @param appName the name of the App
     * @param appParamName the name of the parameter
     * @param envVar the environment variable name
     * @return the environment variable value
     */
    public static String discoverEnvVarValue(final String appName, final String appParamName, final String envVar) {
        return discoverEnvVarValue(() -> guessEnvVar(appName, appParamName), envVar);
    }

    /**
     * Creates a secret based on a given {@link ParamDescriptor}.
     *
     * @param key the key of the App
     * @param paramName the name of the parameter
     * @param value the value of the parameter
     * @param paramDescriptor the parameter descriptor
     * @return a secret object
     */
    private static Secret secretFromDescribedParam(final String key,
                                                   final String paramName,
                                                   final char[] value,
                                                   final ParamDescriptor paramDescriptor) {
        return Optional.ofNullable(paramDescriptor)
                .map(pd -> Secret.builder()
                        .withValue(value)
                        .withHidden(pd.isHidden())
                        .withType(pd.getType())
                        .withEnvVar(pd.getEnvVar())
                        .withEnvShow(pd.getEnvShow())
                        .withEnvValue(discoverEnvVarValue(key, paramName, pd.getEnvVar()))
                        .build())
                .orElse(null);
    }

    /**
     * Builds the host-specific environment variable name for an app param following the
     * {@code DOT_{APP_KEY}_{HOSTNAME}_{APP_VALUE_KEY}} convention.
     * <p>
     * The three segments are concatenated and normalized through {@link Config#envKey(String)}
     * so that casing, separators (dots/dashes) and doubled/trailing underscores are handled by
     * the single canonical normalization routine (no parallel normalization logic). The
     * {@code DOT_} prefix is applied by {@link Config#envKey(String)}.
     * <p>
     * Example: {@code envVarName("dotcms-app", "demo.dotcms.com", "clientId")} resolves to
     * {@code DOT_DOTCMS_APP_DEMO_DOTCMS_COM_CLIENTID}.
     *
     * @param appKey   the registered app key (matches {@code AppDescriptor.getKey()})
     * @param hostName the dotCMS site name
     * @param valueKey the declared param/value key
     * @return the normalized environment variable name
     */
    public static String envVarName(final String appKey, final String hostName, final String valueKey) {
        return Config.envKey(String.join("_", appKey, hostName, valueKey));
    }

    /**
     * Builds a {@link Secret} carrying an environment-sourced value with {@code fromEnv=true},
     * inheriting the param's declared {@link Type} and hidden flag from the descriptor (so SECRET
     * masking is preserved regardless of value source). The raw env string is used verbatim — no
     * type coercion is applied.
     *
     * @param envValue        the raw environment value (already verified non-blank)
     * @param describedParam  the declared param descriptor, or {@code null} when unknown
     * @param locked          when {@code true} the value is stored as the env-var value so that
     *                        {@link AbstractProperty#isEditable()} returns {@code false} (tier-1
     *                        host-specific lock); when {@code false} the field stays editable
     *                        (tier-3 / tier-4 do not lock the UI)
     * @return an env-backed {@link Secret}
     */
    private static Secret envSecret(final String envValue,
                                    final ParamDescriptor describedParam,
                                    final boolean locked) {
        final Type type = describedParam != null ? describedParam.getType() : Type.STRING;
        final boolean hidden = describedParam != null && describedParam.isHidden();
        final Secret.Builder builder = Secret.builder()
                .withType(type)
                .withHidden(hidden)
                .withFromEnv(true);
        if (locked) {
            builder.withEnvValue(envValue);
        } else {
            builder.withValue(envValue);
        }
        return builder.build();
    }

    /**
     * Tier-1 (host-specific env) lookup: {@code DOT_{APP_KEY}_{HOSTNAME}_{APP_VALUE_KEY}}. A present
     * value locks the UI field (read-only) because the infra explicitly targeted this host+param.
     * Construct-and-lookup against a known param name — never parses arbitrary env var names. Reads
     * through {@link Config#getStringProperty(String, String)} — never {@code System.getenv()}.
     *
     * @param appKey         the registered app key (matches {@code AppDescriptor.getKey()})
     * @param hostName       the dotCMS site name
     * @param valueKey       the declared param/value key
     * @param describedParam the declared param descriptor, or {@code null}
     * @return an {@link Optional} env-backed {@link Secret}, or empty when absent/blank
     */
    public static Optional<Secret> hostEnvSecret(final String appKey,
                                                 final String hostName,
                                                 final String valueKey,
                                                 final ParamDescriptor describedParam) {
        // Guard: a site whose name normalizes to the System Host segment (e.g. "system-host") would
        // produce the same env var name as the tier-3 global form. Skip tier-1 for it to avoid the
        // host-specific lookup being indistinguishable from the global one.
        if (Config.envKey(hostName).equals(Config.envKey(SYSTEM_HOST_ENV_SEGMENT))) {
            Logger.warn(AppsUtil.class, String.format(
                    "Site '%s' normalizes to the reserved SYSTEM_HOST env segment; "
                            + "host-specific (tier-1) env provisioning is disabled for it.", hostName));
            return Optional.empty();
        }
        final String envValue = Config.getStringProperty(envVarName(appKey, hostName, valueKey), null);
        if (isNotSet(envValue)) {
            return Optional.empty();
        }
        return Optional.of(envSecret(envValue, describedParam, true));
    }

    /**
     * Tier-3 (global / System Host env) lookup: {@code DOT_{APP_KEY}_SYSTEM_HOST_{APP_VALUE_KEY}}.
     * Does NOT lock the UI field — a host-specific stored value (tier-2) is allowed to win over this
     * global env var (specificity-first precedence).
     *
     * @param appKey         the registered app key
     * @param valueKey       the declared param/value key
     * @param describedParam the declared param descriptor, or {@code null}
     * @return an {@link Optional} env-backed {@link Secret}, or empty when absent/blank
     */
    public static Optional<Secret> systemHostEnvSecret(final String appKey,
                                                       final String valueKey,
                                                       final ParamDescriptor describedParam) {
        final String envValue =
                Config.getStringProperty(envVarName(appKey, SYSTEM_HOST_ENV_SEGMENT, valueKey), null);
        if (isNotSet(envValue)) {
            return Optional.empty();
        }
        return Optional.of(envSecret(envValue, describedParam, false));
    }

    /**
     * Tier-4 (legacy global env) lookup using the pre-existing {@link #guessEnvVar(String, String)}
     * pattern ({@code APP_{APP_KEY}_PARAM_{APP_VALUE_KEY}}, resolved by {@link Config} as
     * {@code DOT_APP_..._PARAM_...}). Retained for backward compatibility; logs a throttled
     * deprecation warning and does NOT lock the UI field.
     *
     * @param appKey         the registered app key
     * @param valueKey       the declared param/value key
     * @param describedParam the declared param descriptor, or {@code null}
     * @return an {@link Optional} env-backed {@link Secret}, or empty when absent/blank
     */
    public static Optional<Secret> legacyEnvSecret(final String appKey,
                                                   final String valueKey,
                                                   final ParamDescriptor describedParam) {
        final String guessed = guessEnvVar(appKey, valueKey);
        final String envValue = Config.getStringProperty(guessed, null);
        if (isNotSet(envValue)) {
            return Optional.empty();
        }
        logLegacyEnvDeprecation(appKey, valueKey, guessed);
        return Optional.of(envSecret(envValue, describedParam, false));
    }

    /**
     * Logs the legacy env-var deprecation warning at most once per JVM per app-key + value-key pair.
     */
    private static void logLegacyEnvDeprecation(final String appKey,
                                                final String valueKey,
                                                final String guessedEnvVar) {
        if (LEGACY_ENV_DEPRECATION_LOGGED.add(appKey + "|" + valueKey)) {
            Logger.warn(AppsUtil.class, String.format(
                    "App secret resolved from deprecated env var '%s' for app '%s' param '%s'. "
                            + "Migrate to DOT_{APP_KEY}_{HOSTNAME}_{APP_VALUE_KEY} "
                            + "(or DOT_{APP_KEY}_SYSTEM_HOST_{APP_VALUE_KEY} for the global form).",
                    Config.envKey(guessedEnvVar), appKey, valueKey));
        }
    }

    /**
     * Guesses the environment variable name based on the key and the parameter name.
     *
     * @param key the key of the App
     * @param paramName the name of the parameter
     * @return the environment variable name
     */
    private static String guessEnvVar(final String key, final String paramName) {
        final String normalizedKey =
                StringUtils.joinOneCharElements(
                    StringUtils.convertCamelToSnake(key
                                    .replace("-config", StringPool.BLANK)
                                    .replaceAll(StringPool.DASH, StringPool.UNDERLINE))
                            .replace("dot_", StringPool.BLANK)
                            .toUpperCase());
        final String normalizedParam =
                StringUtils.joinOneCharElements(StringUtils.convertCamelToSnake(paramName).toUpperCase());
        return String.format(APP_PARAM_ENV_VAR_TEMPLATE, normalizedKey, normalizedParam);
    }

    /**
     * Discovers the environment variable value based on the given environment variable name.
     *
     * @param envVarSupplier the environment variable supplier to return the guessed environment variable name
     * @param envVar the environment variable name
     * @return the environment variable value
     */
    private static String discoverEnvVarValue(final Supplier<String> envVarSupplier, final String envVar) {
        return Optional
                .ofNullable(envVarSupplier.get())
                .map(supplied -> Config.getStringProperty(supplied, null))
                .or(() -> Optional.ofNullable(envVar).map(ev -> Config.getStringProperty(ev, null)))
                .or(() -> Optional.ofNullable(envVar).map(System::getenv))
                .orElse(null);
    }

    /**
     * Returns the Secrets of a specific App for a given Site in dotCMS.
     * This is done by evaluating searching for a secret with the given param name and if found evaluates that
     * it is required, editable and has no value set.
     * If the secret is not found then just evaluate the required flag.
     *
     * @param descriptorParam descriptor param
     * @param appSecretsOptional app secrets
     * @param paramName param name
     * @return true if the param is required otherwise false
     */
    private static boolean isRequired(final Entry<String, ParamDescriptor> descriptorParam,
                                      final Optional<AppSecrets> appSecretsOptional,
                                      final String paramName) {
        final Optional<Secret> secret =
                appSecretsOptional.map(appSecrets -> appSecrets.getSecrets().get(paramName));
        return secret.map(value -> descriptorParam.getValue().isRequired()
                        && value.isEditable()
                        && Objects.nonNull(value.getValue()))
                .orElseGet(() -> descriptorParam.getValue().isRequired());
    }

}
