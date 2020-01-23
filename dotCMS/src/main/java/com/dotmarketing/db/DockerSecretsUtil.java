package com.dotmarketing.db;

import com.dotmarketing.exception.DotRuntimeException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Original code was taken from https://github.com/carsdotcom/docker-secrets-java/blob/master/src/main/java/com/cars/framework/secrets/DockerSecrets.java
 * and modified to our needs (License type: Apache 2.0)
 * @author github.com/carsdotcom
 */
public class DockerSecretsUtil {

    private static final String SECRETS_DIR = "/run/secrets/";

    public static Map<String, String> load() throws DotRuntimeException {
        File secretsDir = new File(SECRETS_DIR);
        return load(secretsDir);
    }

    public static Map<String, String> loadFromFile(final String fileName) throws DotRuntimeException {
        File secretsFile;
        if (Paths.get(fileName).isAbsolute()){
            secretsFile = new File(fileName);
        }else{
            secretsFile = new File(SECRETS_DIR + fileName);
        }

        return loadFromFile(secretsFile);
    }

    public static Map<String, String> loadFromFile(final File secretsFile)
            throws DotRuntimeException {

        if (!secretsFile.exists()) {
            throw new DotRuntimeException(
                    "Unable to read secrets from file at [" + secretsFile.toPath() + "]");
        }

        final Map<String, String> secrets = new HashMap<>();

        try {
            final List<String> lines = Files.readAllLines(secretsFile.toPath(), Charset.defaultCharset());
            for (String line : lines) {
                int index = line.indexOf("=");
                if (index < 0) {
                    throw new DotRuntimeException(
                            "Invalid secrets in file at [" + secretsFile.toPath() + "]");
                }
                final String key = line.substring(0, index);
                final String value = line.substring(index + 1);
                secrets.put(key, value);
            }
        } catch (IOException e) {
            throw new DotRuntimeException(
                    "Unable to read secrets from file at [" + secretsFile.toPath() + "]");
        }
        return secrets;

    }


    public static Map<String, String> load(final File secretsDir) throws DotRuntimeException {

        if (!secretsDir.exists()) {
            throw new DotRuntimeException("Unable to find any secrets under [" + SECRETS_DIR + "]");
        }

        final File[] secretFiles = secretsDir.listFiles();

        if (secretFiles == null || secretFiles.length == 0) {
            throw new DotRuntimeException("Unable to find any secrets under [" + SECRETS_DIR + "]");
        }

        final Map<String, String> secrets = new HashMap<>();

        for (final File file : secretFiles) {
            try {
                final String secret = new String(Files.readAllBytes(file.toPath()));
                secrets.put(file.getName(), secret);
            } catch (IOException e) {
                throw new DotRuntimeException(
                        "Unable to load secret from file [" + file.getName() + "]", e);
            }
        }
        return secrets;
    }
}
