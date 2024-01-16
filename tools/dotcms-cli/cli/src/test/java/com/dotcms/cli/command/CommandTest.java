package com.dotcms.cli.command;

import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.model.config.ServiceBean;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import picocli.CommandLine;

public abstract class CommandTest {

    protected static final String PICOCLI_ANSI = "picocli.ansi";

    /**
     * This here to prevent an issue running test from within quarkus:dev console on which coloring/styling meta chars are not resolved. Therefore, this breaks our tests.
     */
    void disableAnsi() {
        System.setProperty(PICOCLI_ANSI, Boolean.FALSE.toString());
    }

    /**
     * This removes the flag to prevent issues outside the test execution
     */
     void enableAnsi() {
        System.clearProperty(PICOCLI_ANSI);
    }

    @Inject
    PicocliCommandLineFactory factory;

    @Inject
    ServiceManager serviceManager;

    @CanIgnoreReturnValue
    protected ServiceManager resetServiceProfiles() throws IOException {
        return serviceManager.removeAll()
                .persist(ServiceBean.builder().name("default").active(true).build());
    }

    protected CommandLine createCommand() {
        final CustomConfiguration customConfiguration = new CustomConfiguration();
        return customConfiguration.customCommandLine(factory);
    }

    @PostConstruct
    public void  postConstruct(){
       disableAnsi();
    }

    @PreDestroy
    public void  preDestroy(){
        enableAnsi();
    }


    /**
     * Creates a temporary folder with a random name.
     *
     * @return The path to the created temporary folder.
     * @throws IOException If an I/O error occurs while creating the temporary folder.
     */
    protected Path createTempFolder() throws IOException {

        String randomFolderName = "folder-" + UUID.randomUUID();
        return Files.createTempDirectory(randomFolderName);
    }

    /**
     * Deletes a temporary directory and all its contents.
     *
     * @param folderPath The path to the temporary directory to be deleted.
     * @throws IOException If an I/O error occurs while deleting the directory or its contents.
     */
    protected  void deleteTempDirectory(Path folderPath) throws IOException {
        Files.walkFileTree(folderPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file); // Deletes the file
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir); // Deletes the directory after its content has been deleted
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
