package com.dotcms.cli.common;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@QuarkusTest
class UtilsTest {

    /**
     * Here we test that {@link Utils#nextFileName(Path)} generates a numerated file when another file already exists
     *
     * @throws IOException
     */
    @Test
    void Test_Next_FileName() throws IOException {
        final String fileName = String.format("my-file%d.something", System.currentTimeMillis());
        final Path path = Path.of(fileName);
        Assertions.assertFalse(Files.exists(path));
        Path next0 = Utils.nextFileName(path);
        Assertions.assertEquals(path.toString(), next0.toString());
        Files.writeString(path, RandomStringUtils.random(10));
        Assertions.assertTrue(Files.exists(path));
        List<Path> files = new ArrayList<>(11);
        files.add(path);
        try {
            for (int i = 1; i <= 10; i++) {
                //after this point the file exists.
                //so any subsequent calls to nextFileName should get me a name followed by a consecutive
                Path next = Utils.nextFileName(path);
                Assertions.assertTrue(next.toString().contains("(" + i + ")"));
                Files.writeString(next, RandomStringUtils.random(10));
                files.add(next);
            }
        } finally {
            for (Path f : files) {
                Files.deleteIfExists(f);
            }
        }
    }

}
