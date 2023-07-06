package com.dotmarketing.microprofile.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ResourceFileManagerBean {

    public static final String WEB_INF_CLASSES = "WEB-INF/classes";
    @Inject
    @ConfigProperty(name = "dot.resource-file-manager.auto-extract-paths",
            defaultValue = "WEB-INF/velocity/.*,toolbox.xml")
    Optional<Set<Pattern>> webAppAutoExtractPatterns;


    @Inject
    @ConfigProperty(name = "dot.resource-file-manager.auto-extract-paths")
    Optional<Set<Pattern>> webAppOverwritePatterns;

    @Inject
    @ConfigProperty(name = "dot.resource-file-manager.auto-extract-paths",
            defaultValue = "log4j2-tomcat.xml")
    Optional<Set<Pattern>> resourcesAutoExtractPatterns;

    @Inject
    @ConfigProperty(name = "dot.resource-file-manager.auto-extract-paths",
            defaultValue = "toolbox.xml")
    Optional<Set<Pattern>> resourcesOverwritePatterns;

    @Inject
    @ConfigProperty(defaultValue = "true")
    Boolean enableCache;

    @Inject
    @ConfigProperty(defaultValue = "override-root-test")
    File webAppOverrideRootDir;
    @Inject
    @ConfigProperty
    Optional<File> overrideRootDir;

    @Inject
    @ConfigProperty(defaultValue = "cache-dir-test")
    File cacheDir;


    private boolean isSourceBuild=false;
    private boolean isGradle=false;
    private boolean isMaven=false;

    private Optional<Path> developmentRoot = Optional.empty();

    private Optional<Path> webAppRoot = Optional.empty();

    private  static final String GRADLE_CLASSES = "build/classes";
    private static final String MAVEN_CLASSES = "target/classes";
    private List<String> pathElements;
    private Optional<Path> resourcesRoot;


    private static List<String> loadClassPathElements() {
        final String classPath = System.getProperty("java.class.path", ".");
        final String[] classPathElements = classPath.split(System.getProperty("path.separator"));
        return Arrays.asList(classPathElements);
    }


    private void findDevelopmentRoot() {

        Optional<Path> explodedWebApp = Optional.empty();
        Optional<Path> foundWebAppRoot = Optional.empty();
        Optional<Path> foundDevelopmentRoot = Optional.empty();
        Optional<Path> foundResources = Optional.empty();
        for (String element : pathElements) {
            if (element.endsWith(".jar")) {
                continue;
            }
            if (element.contains(GRADLE_CLASSES) && foundDevelopmentRoot.isEmpty()) {
                isGradle=true;
                isSourceBuild=true;
                foundResources = Optional.of(Path.of(element));
                foundDevelopmentRoot = Optional.of(Path.of(element.substring(0, element.indexOf(GRADLE_CLASSES))));
                foundWebAppRoot = foundDevelopmentRoot.map(root -> root.resolve("src/main/webapp"));

            } else if (element.contains(MAVEN_CLASSES) && foundDevelopmentRoot.isEmpty())
            {
                isMaven=true;
                isSourceBuild=true;
                foundResources = Optional.of(Path.of(element));
                foundDevelopmentRoot =  Optional.of(Path.of(element.substring(0, element.indexOf(MAVEN_CLASSES))));
                foundWebAppRoot = foundDevelopmentRoot.map(root -> root.resolve("src/main/webapp"));
            } else if (element.contains(WEB_INF_CLASSES) && explodedWebApp.isEmpty())
            {
                foundResources = Optional.of(Path.of(element));
                explodedWebApp = Optional.of(Path.of(element.substring(0, element.indexOf(WEB_INF_CLASSES))));
            }
        }
        developmentRoot = foundDevelopmentRoot;
        Optional<Path> finalExplodedWebApp = explodedWebApp;
        webAppRoot = foundWebAppRoot.or(() -> finalExplodedWebApp);
        resourcesRoot = foundResources;
    }


    ClassLoader classLoader;

    public ResourceFileManagerBean() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = this.getClass().getClassLoader();
        }
        classLoader = loader;
    }

    @PostConstruct
    public void initializeCache() {
        System.out.println("Initializing ResourceFileManagerBean");
        pathElements = loadClassPathElements();
        findDevelopmentRoot();

        setupOverrideRoot();

        System.out.println("isSourceBuild: " + isSourceBuild);
        System.out.println("isGradle: " + isGradle);
        System.out.println("isMaven: " + isMaven);
        System.out.println("developmentRoot: " + developmentRoot);
        System.out.println("webAppRoot: " + webAppRoot);
        System.out.println("autoExtractPaths: " + webAppAutoExtractPatterns);
        System.out.println("enableCache: " + enableCache);
        System.out.println("overrideRootDir: " + overrideRootDir);
        System.out.println("cacheDir: " + cacheDir);

    }

    private void setupOverrideRoot() {


        if (webAppRoot.isPresent())
        {
            try {
                copyFolder(webAppRoot.get(), webAppOverrideRootDir.toPath(), webAppAutoExtractPatterns.orElse(
                        Collections.EMPTY_SET),webAppOverwritePatterns.orElse(Collections.emptySet()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }



        if (resourcesRoot.isPresent())
        {
            try {
                copyFolder(resourcesRoot.get(),overrideRootDir.orElse( webAppOverrideRootDir.toPath().resolve(WEB_INF_CLASSES).toFile()).toPath() , resourcesAutoExtractPatterns.orElse(Collections.emptySet()), resourcesOverwritePatterns.orElse(Collections.emptySet()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }



    }

    /*
       Copy folder including files and sub-folders that match one of the patterns
     */
    public void copyFolder(Path src, Path dest, Set<Pattern> patterns, Set<Pattern> overwritePatterns) throws IOException {
        Files.walk(src)
                .filter(path -> doesFolderMatchPattern(src, patterns, path))
                .forEach(path -> {
                    try {
                        Path destPath = dest.resolve(src.relativize(path));
                        if (Files.isDirectory(path)) {
                            if (!Files.exists(destPath)) {
                                Files.createDirectories(destPath);
                            }
                        } else {
                            if (!Files.exists(destPath.getParent())) {
                                Files.createDirectories(destPath.getParent());
                            }
                            if (!Files.exists(destPath) || doesFolderMatchPattern(src, overwritePatterns, path)) {
                                Files.copy(path, destPath, StandardCopyOption.REPLACE_EXISTING);
                            } else {
                                System.out.println("Existing file: " + destPath);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private static boolean doesFolderMatchPattern(Path src, Set<Pattern> patterns, Path path) {
        return patterns.stream()
                .anyMatch(pattern -> pattern.matcher(src.relativize(path).toString()).matches());
    }

    public Collection<String> getResources(
            final Pattern pattern, boolean webApp){
        final ArrayList<String> retval = new ArrayList<String>();
        for(final String element : pathElements){
            retval.addAll(getResources(element, pattern, webApp));
        }
        return retval;
    }

    private static Collection<String> getResources(
            final String element,
            final Pattern pattern, boolean webApp){
        final ArrayList<String> retval = new ArrayList<>();
        final File file = new File(element);
        if(file.isDirectory()){
            retval.addAll(getResourcesFromDirectory(file, pattern,webApp));
        } else {
            retval.addAll(getResourcesFromJarFile(file, pattern,webApp));
        }
        return retval;
    }
    private static Collection<String> getResourcesFromJarFile(
            final File file,
            final Pattern pattern, boolean webApp){
        final ArrayList<String> retval = new ArrayList<String>();
        ZipFile zf;
        try{
            zf = new ZipFile(file);
        } catch(final ZipException e){
            throw new Error(e);
        } catch(final IOException e){
            throw new Error(e);
        }
        final Enumeration e = zf.entries();
        while(e.hasMoreElements()){
            final ZipEntry ze = (ZipEntry) e.nextElement();
            final String fileName = ze.getName();
            final boolean accept = pattern.matcher(fileName).matches();
            if(accept){
                retval.add(fileName);
            }
        }
        try{
            zf.close();
        } catch(final IOException e1){
            throw new Error(e1);
        }
        return retval;
    }

    private static Collection<String> getResourcesFromDirectory(
            final File directory,
            final Pattern pattern, boolean webApp){
        final ArrayList<String> retval = new ArrayList<String>();
        final File[] fileList = directory.listFiles();
        for(final File file : fileList){
            if(file.isDirectory()){
                retval.addAll(getResourcesFromDirectory(file, pattern,webApp));
            } else{
                try{
                    final String fileName = file.getCanonicalPath();
                    final boolean accept = pattern.matcher(fileName).matches();
                    if(accept){
                        retval.add(fileName);
                    }
                } catch(final IOException e){
                    throw new Error(e);
                }
            }
        }
        return retval;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public File getResource(String path) {
        return null;
    }

    public File getWebAppFile(String path) {
        return null;
    }
}
