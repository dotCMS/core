package com.liferay.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.util.CloseUtils;
import com.dotmarketing.util.Config;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FileUtilTest {

    @Before
    public void setUp(){
        Config.initializeConfig();
    }

    @Test
    public void test_uncompress_outputstream()  throws Exception {

        final File file = com.dotmarketing.util.FileUtil.createTemporaryFile("nocompre_");
        final OutputStream out = FileUtil.createOutputStream(file);

        final String  string = "testuncompress";
        final byte [] bytes  = string.getBytes();
        out.write(bytes);
        CloseUtils.closeQuietly(out);

        final InputStream in   = FileUtil.createInputStream(file);
        final byte [] buff     = new byte[bytes.length];
        in.read(buff);
        CloseUtils.closeQuietly(in);

        Assert.assertEquals("The file recovery should be the file saved", new String(buff), string);
    }

    @Test
    public void test_compress_gzip_outputstream()  throws Exception {

        final File file = com.dotmarketing.util.FileUtil.createTemporaryFile("gzip");
        final OutputStream out = FileUtil.createOutputStream(file, "gzip");

        final String  string = "testcompress";
        final byte [] bytes  = string.getBytes();
        out.write(bytes);
        CloseUtils.closeQuietly(out);

        final InputStream in   = FileUtil.createInputStream(file, "gzip");
        final byte [] buff     = new byte[bytes.length];
        in.read(buff);
        CloseUtils.closeQuietly(in);

        Assert.assertEquals("The file recovery should be the file saved", new String(buff), string);
    }

    @Test
    public void test_compress_gzip_enum_outputstream()  throws Exception {

        final File file = com.dotmarketing.util.FileUtil.createTemporaryFile("egzip");
        final OutputStream out = FileUtil.createOutputStream(file.toPath(), FileUtil.StreamCompressorType.GZIP);

        final String  string = "testcompress";
        final byte [] bytes  = string.getBytes();
        out.write(bytes);
        CloseUtils.closeQuietly(out);

        final InputStream in   = FileUtil.createInputStream(file.toPath(), FileUtil.StreamCompressorType.GZIP);
        final byte [] buff     = new byte[bytes.length];
        in.read(buff);
        CloseUtils.closeQuietly(in);

        Assert.assertEquals("The file recovery should be the file saved", new String(buff), string);
    }

    @Test
    public void test_compress_bzip2_outputstream()  throws Exception {

        final File file = com.dotmarketing.util.FileUtil.createTemporaryFile("bzip2");
        final OutputStream out = FileUtil.createOutputStream(file, "bzip2");

        final String  string = "testcompress";
        final byte [] bytes  = string.getBytes();
        out.write(bytes);
        CloseUtils.closeQuietly(out);

        final InputStream in   = FileUtil.createInputStream(file, "bzip2");
        final byte [] buff     = new byte[bytes.length];
        in.read(buff);
        CloseUtils.closeQuietly(in);

        Assert.assertEquals("The file recovery should be the file saved", new String(buff), string);
    }

    @Test
    public void test_compress_bzip2_enum_outputstream()  throws Exception {

        final File file = com.dotmarketing.util.FileUtil.createTemporaryFile("ebzip2");
        final OutputStream out = FileUtil.createOutputStream(file.toPath(), FileUtil.StreamCompressorType.BZIP2);

        final String  string = "testcompress";
        final byte [] bytes  = string.getBytes();
        out.write(bytes);
        CloseUtils.closeQuietly(out);

        final InputStream in   = FileUtil.createInputStream(file.toPath(), FileUtil.StreamCompressorType.BZIP2);
        final byte [] buff     = new byte[bytes.length];
        in.read(buff);
        CloseUtils.closeQuietly(in);

        Assert.assertEquals("The file recovery should be the file saved", new String(buff), string);
    }

    @Test
    public void test_clean_tree_deletes_folders_files_by_date()  throws Exception {

        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);

        List<Date> hours = ImmutableList.of(
                cal.getTime(),                                     // now
                new Date(cal.getTimeInMillis()-(60*60*1000)),      // 1 hour ago
                new Date(cal.getTimeInMillis()-(2*60*60*1000)),    // 2 hours ago
                new Date(cal.getTimeInMillis()-(5*60*60*1000)),    // 5 hours ago
                new Date(cal.getTimeInMillis()-(10*60*60*1000)));  // 10 hours ago

        final File tempDir =  Files.createTempDirectory("temp").toFile();

        tempDir.mkdirs();
        FileUtil.deltree(tempDir, false);
        // a clean start
        assertEquals(0,FileUtil.listFilesRecursively(tempDir).size());

        for(int i=0;i<5;i++) {
            final File parent = new File(tempDir,"folder" + i);
            parent.mkdirs();
            final File child = new File(parent,"file" + i);
            new FileOutputStream(child).close();
            child.setLastModified(hours.get(i).getTime());
            parent.setLastModified(hours.get(i).getTime());
        }

        // create an old folder with a new file in it
        File parent = new File(tempDir,"folderxx");
        parent.mkdirs();

        File child = new File(parent,"filex");
        new FileOutputStream(child).close();
        child.setLastModified(System.currentTimeMillis());
        parent.setLastModified(hours.get(4).getTime());

        assertEquals(12,FileUtil.listFilesRecursively(tempDir).size());

        FileUtil.cleanTree(tempDir,hours.get(3));

        assertEquals(10,FileUtil.listFilesRecursively(tempDir).size());

        FileUtil.cleanTree(tempDir,hours.get(2));


        assertEquals(8,FileUtil.listFilesRecursively(tempDir).size());

        FileUtil.cleanTree(tempDir,hours.get(0));
        assertEquals(4,FileUtil.listFilesRecursively(tempDir).size());


    }

    /**
     * Simple test to validate FileUtil.move
     * Scenario: we create a non-empty file under a temp dir also we send an empty file as the destination marker
     * Expected: The source file should get moved into the empty destination marker
     * It is important noticing this method expects in both params a file
     * @throws Exception
     */
    @Test
    public void simpleMoveTest() throws Exception {

        final Path sourceDir = Files.createTempDirectory("test-move-source");
        final Path destDir = Files.createTempDirectory("test-move-dest");

        final Path sourceFile = Files.createTempFile(sourceDir, "source", "foo");
        Files.write(sourceFile, RandomStringUtils.randomAlphanumeric(100).getBytes());

        final Path destFile = Files.createTempFile(destDir, "dest", "foo");
        final File source = sourceFile.toFile();
        BasicFileAttributes sourceAttributes = Files.readAttributes(source.toPath(), BasicFileAttributes.class);
        final long length = sourceAttributes.size();
        final long lastModified = sourceAttributes.lastModifiedTime().toMillis();

        Assert.assertTrue(FileUtil.move(source, destFile.toFile()));
        try (
                final Stream<Path> stream = Files.list(destDir)) {
            final List<Path> paths = stream.collect(Collectors.toList());
            Assert.assertFalse(paths.isEmpty());
            final File moved = paths.get(0).toFile();
            Assert.assertTrue(moved.exists());

            //We're testing copy so the source shouldn't exist anymore
            Assert.assertFalse(source.exists());

            BasicFileAttributes destAttributes = Files.readAttributes(moved.toPath(), BasicFileAttributes.class);
            assertEquals(length, destAttributes.size());
            assertEquals(lastModified, destAttributes.lastModifiedTime().toMillis());
        }
    }

    /**
     * Simple test to validate FileUtil.listFiles
     * Scenario: we create a non-empty file under a temp dir
     * Expected: we should get a list with one single entry regardless of the recursive param
     * @throws Exception
     */
    @Test
    public void simpleListFilesTest() throws Exception {
        final Path sourceDir = Files.createTempDirectory("test-move-source");
        final Path sourceFile = Files.createTempFile(sourceDir, "source", "foo");
        Files.write(sourceFile, RandomStringUtils.randomAlphanumeric(100).getBytes());
        final String[] files1 = FileUtil.listFiles(sourceDir.toFile(), true);
        Assert.assertEquals(1, files1.length);
        final String[] files2 = FileUtil.listFiles(sourceDir.toFile(), false);
        Assert.assertEquals(1, files2.length);
    }

    /**
     * Simple test to validate FileUtil.listFiles
     * Scenario: we create a non-empty file under a temp dir
     * Expected: we should get a list with one single entry regardless of the recursive param
     * @throws Exception
     */
    @Test
    public void simpleListFileHandlesTest() throws Exception {
        final Path sourceDir = Files.createTempDirectory("test-move-source");
        final Path sourceFile = Files.createTempFile(sourceDir, "source", "foo");
        Files.write(sourceFile, RandomStringUtils.randomAlphanumeric(100).getBytes());
        final File[] files1 = FileUtil.listFileHandles(sourceDir.toFile(),true);
        Assert.assertEquals(1, files1.length);
        final File[] files2 = FileUtil.listFileHandles(sourceDir.toFile(),false);
        Assert.assertEquals(1, files2.length);
    }

    /**
     * Test to validate FileUtil.getFilesByPattern with nested directory structure.
     * Verifies that files within multiple levels of subfolders are retrieved correctly
     * using pattern matching instead of the deprecated listFileHandles method.
     * @throws Exception
     */
    @Test
    public void testGetFilesByPatternRecursive() throws Exception {
        final Path rootDir = Files.createTempDirectory("test-listFileHandles-recursive");
        try {
            // Create nested directory structure
            final Path level1Dir = Files.createDirectory(rootDir.resolve("level1"));
            final Path level2Dir = Files.createDirectory(level1Dir.resolve("level2"));
            final Path level3Dir = Files.createDirectory(level2Dir.resolve("level3"));

            // Create files at root level
            final Path rootFile1 = Files.createFile(rootDir.resolve("root_file1.txt"));
            final Path rootFile2 = Files.createFile(rootDir.resolve("root_file2.log"));
            Files.write(rootFile1, "Root file 1 content".getBytes());
            Files.write(rootFile2, "Root file 2 content".getBytes());

            // Create files at level 1
            final Path level1File1 = Files.createFile(level1Dir.resolve("level1_file1.txt"));
            final Path level1File2 = Files.createFile(level1Dir.resolve("level1_file2.json"));
            Files.write(level1File1, "Level 1 file 1 content".getBytes());
            Files.write(level1File2, "{\"test\": \"level 1 file 2\"}".getBytes());

            // Create files at level 2
            final Path level2File1 = Files.createFile(level2Dir.resolve("level2_file1.xml"));
            final Path level2File2 = Files.createFile(level2Dir.resolve("level2_file2.properties"));
            Files.write(level2File1, "<xml>Level 2 file 1</xml>".getBytes());
            Files.write(level2File2, "key=level2file2".getBytes());

            // Create files at level 3
            final Path level3File1 = Files.createFile(level3Dir.resolve("level3_file1.txt"));
            final Path level3File2 = Files.createFile(level3Dir.resolve("level3_file2.csv"));
            Files.write(level3File1, "Level 3 file 1 content".getBytes());
            Files.write(level3File2, "header1,header2\nvalue1,value2".getBytes());

            // Test pattern matching for all files (*)
            final Collection<File> allFiles = FileUtil.getFilesByPattern(rootDir.toFile(), "*");

            // Should find all 8 files across all directory levels
            Assert.assertEquals("Should find all 8 files with * pattern", 8, allFiles.size());

            // Convert to file names for easier verification
            Set<String> fileNames = allFiles.stream()
                    .map(File::getName)
                    .collect(Collectors.toSet());

            // Verify files from each level are included
            Assert.assertTrue("Should contain root level txt file", fileNames.contains("root_file1.txt"));
            Assert.assertTrue("Should contain root level log file", fileNames.contains("root_file2.log"));
            Assert.assertTrue("Should contain level 1 txt file", fileNames.contains("level1_file1.txt"));
            Assert.assertTrue("Should contain level 1 json file", fileNames.contains("level1_file2.json"));
            Assert.assertTrue("Should contain level 2 xml file", fileNames.contains("level2_file1.xml"));
            Assert.assertTrue("Should contain level 2 properties file", fileNames.contains("level2_file2.properties"));
            Assert.assertTrue("Should contain level 3 txt file", fileNames.contains("level3_file1.txt"));
            Assert.assertTrue("Should contain level 3 csv file", fileNames.contains("level3_file2.csv"));

            // Test pattern matching for specific file types
            final Collection<File> txtFiles = FileUtil.getFilesByPattern(rootDir.toFile(), "*.txt");

            // Should find 3 txt files across all directory levels
            Assert.assertEquals("Should find 3 txt files with *.txt pattern", 3, txtFiles.size());

            Set<String> txtFileNames = txtFiles.stream()
                    .map(File::getName)
                    .collect(Collectors.toSet());

            // Should only contain txt files
            Assert.assertTrue("Should contain root txt file", txtFileNames.contains("root_file1.txt"));
            Assert.assertTrue("Should contain level 1 txt file", txtFileNames.contains("level1_file1.txt"));
            Assert.assertTrue("Should contain level 3 txt file", txtFileNames.contains("level3_file1.txt"));

            // Should not contain non-txt files
            Assert.assertFalse("Should not contain log files", txtFileNames.contains("root_file2.log"));
            Assert.assertFalse("Should not contain json files", txtFileNames.contains("level1_file2.json"));
            Assert.assertFalse("Should not contain xml files", txtFileNames.contains("level2_file1.xml"));

        } finally {
            // Clean up
            FileUtil.deltree(rootDir.toFile());
        }
    }

    /**
     * Simple test to validate FileUtil.copyFile
     * Scenario: we create two files source and dest then we exec copy
     * Expected: both file should be "equal" in terms of size and modifications time. Source should continue to exist
     * @throws Exception
     */
    @Test
    public void simpleCopyFilesTest() throws Exception {

        final Path sourceDir = Files.createTempDirectory("test-move-source");
        final Path sourceFile = Files.createTempFile(sourceDir, "source", "foo");
        Files.write(sourceFile, RandomStringUtils.randomAlphanumeric(100).getBytes());
        final Path destFile = Files.createTempFile(sourceDir, "empty-dest", "foo");
        FileUtil.copyFile(sourceFile.toFile(), destFile.toFile());

        BasicFileAttributes sourceAttributes = Files.readAttributes(sourceFile, BasicFileAttributes.class);
        BasicFileAttributes destAttributes = Files.readAttributes(destFile, BasicFileAttributes.class);
        Assert.assertEquals(sourceAttributes.size(), destAttributes.size());
        Assert.assertEquals(sourceAttributes.lastModifiedTime(), destAttributes.lastModifiedTime());
        //We're testing copy so the source should still exist
        Assert.assertTrue(sourceFile.toFile().exists());
    }

    /**
     * Method to test: {@link FileUtil#copyDirectory(File, File)}
     * Scenario: We create a parent dir/file structure including subdirectories
     * Expected: after calling copyDirectory we verify that whole dir/files structure was moved according
     * @throws Exception
     */
    @Test
    public void simpleCopyDirsTest() throws Exception {

        //Root folder
        final Path sourceRootDir1 = Files.createTempDirectory("test-move-source");

        //with one file
        final Path underRoot1 = Files.createTempFile(sourceRootDir1, "source", "foo");
        Files.write(underRoot1, RandomStringUtils.randomAlphanumeric(100).getBytes());

        final Path dir1 = Files.createTempDirectory(sourceRootDir1, "test-move-source");
        final Path underDir1 = Files.createTempFile(dir1, "source", "foo");
        Files.write(underDir1, RandomStringUtils.randomAlphanumeric(100).getBytes());

        final Path destDir = Files.createTempDirectory("destDir");

        Assert.assertTrue(FileUtil.listFilesRecursively(destDir.toFile()).isEmpty());
        Assert.assertEquals(0, FileUtil.listFiles(destDir.toFile(),true).length);

        FileUtil.copyDirectory(sourceRootDir1.toFile(), destDir.toFile());

        //I don't find this method reliable
        Assert.assertFalse(FileUtil.listFilesRecursively(destDir.toFile()).isEmpty());
        Assert.assertFalse(FileUtil.listFilesRecursively(destDir.toFile(), File::isFile).isEmpty());

        //So I'm testing the copy method using core java
        try (
                final Stream<Path> stream = Files.list(destDir)) {
            final List<Path> paths = stream.collect(Collectors.toList());

            Assert.assertFalse(paths.isEmpty());
            Assert.assertEquals(1, paths.stream().filter(path -> path.toFile().isDirectory()).count());
            Assert.assertEquals(1, paths.stream().filter(path -> path.toFile().isFile()).count());

            final Optional<Path> optionalDir = paths.stream().filter(path -> path.toFile().isDirectory()).findFirst();
            Assert.assertTrue(optionalDir.isPresent());
            final Optional<Path> optionalFile = paths.stream().filter(path -> path.toFile().isFile()).findFirst();
            Assert.assertTrue(optionalFile.isPresent());

            final Path dir = optionalDir.get();

            try(
                    final Stream<Path> list = Files.list(dir);
            ){
                final List<Path> subPaths = list.collect(Collectors.toList());
                Assert.assertFalse(subPaths.isEmpty());
            }
        }
    }

    @Test
    public void test_no_NPE_on_copy() throws Exception {

        try{
            com.liferay.util.FileUtil.copyFile(null, null, false);
        }catch(Exception e){
            assertTrue("Error is an IOException", e instanceof IOException);
            return;
        }
        fail("This should have thrown an error");

    }

}
