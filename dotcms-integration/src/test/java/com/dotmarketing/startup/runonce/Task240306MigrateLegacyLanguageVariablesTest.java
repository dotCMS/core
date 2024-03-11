package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task240306MigrateLegacyLanguageVariablesTest {

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException, IOException, URISyntaxException {
        final Task240306MigrateLegacyLanguageVariables upgradeTask = new Task240306MigrateLegacyLanguageVariables();
        seedTest(upgradeTask.messagesDir());
        assertTrue(upgradeTask.forceRun());
        upgradeTask.executeUpgrade();
    }

    private static void seedTest(final Path messagesDir)
            throws URISyntaxException, IOException {
        final URL resource = Thread.currentThread().getContextClassLoader().getResource("lang-vars");
        if(resource == null){
            throw new RuntimeException("lang-vars directory not found");
        }
        final Path resourcesPath = Path.of(resource.toURI());
        if (!Files.exists(messagesDir)){
            Files.createDirectories(messagesDir);
        }
        copyDir(resourcesPath, messagesDir);
    }


    public static void copyDir(Path src, Path dest) throws IOException {
        try (Stream<Path> stream = Files.walk(src)) {
            // Iterate over each Path object in the stream
            stream.forEach(source -> {
                // Get the relative path from the source directory
                Path relativePath = src.relativize(source);

                // Get the corresponding path in the destination directory
                Path destination = dest.resolve(relativePath);

                try {
                    // Copy each Path object from source to destination
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    Logger.error(Task240306MigrateLegacyLanguageVariablesTest.class, e.getMessage(), e);
                }
            });
        } catch (IOException e) {
            Logger.error(Task240306MigrateLegacyLanguageVariablesTest.class, e.getMessage(), e);
        }
    }

}
