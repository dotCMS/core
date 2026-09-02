package com.dotcms.rest.api.v1.taillog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityListStringView;
import com.dotcms.rest.api.v1.taillog.TailLogResource.MyTailerListener;
import com.dotcms.rest.api.v1.taillog.TailLogResource.MyTailerThread;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TailLogResourceTest {

    static final String TAILING = "Tailing info from file";
    static final String WRITTEN = "New Line written With Number";

    private static TailLogResource resource;
    private static HttpServletResponse mockResponse;
    private static User adminUser;
    private static User nonAdminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        resource = new TailLogResource();
        mockResponse = new MockHttpResponse().response();
        adminUser = TestUserUtils.getAdminUser();

        nonAdminUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(
                APILocator.getRoleAPI().loadBackEndUserRole(), nonAdminUser);
    }

    synchronized void writeText(final Writer out, final int index) throws IOException {
        out.write(String.format("%s (%d).  %n", WRITTEN, index));
        out.flush();
    }

    /**
     * <b>Method to Test:</b> {@link MyTailerThread#run()}<br></br>
     * <b>When:</b>  an instance of {@link MyTailerThread} is executed<br></br>
     * <b>Should:</b>  read lines attached to a listener adapter and send them to an {@link OutboundEvent}
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testTailLogResource() throws IOException, InterruptedException {

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final File file = File.createTempFile("tailLog", ".txt");

        // Create a listener and an event output
        final MyTailerListener listener = new MyTailerListener();
        final List<Tuple2<String,Map<String,?>>> collectedEvents = new ArrayList<>();

        final EventOutput eventOutput = new EventOutput(){
            @Override
            @SuppressWarnings("unchecked")
            public void write(final OutboundEvent outboundEvent) throws IOException {
                super.write(outboundEvent);
                final String name = outboundEvent.getName();
                final Object data = outboundEvent.getData();
                Logger.info(TailLogResourceTest.class,String.format("EventOutput.write() called with arg [%s].", name));
                collectedEvents.add(Tuple.of(name, (Map<String, ?>) data));
            }
        };
        final MyTailerThread tailerThread = new MyTailerThread(file, listener, eventOutput, 300);
        tailerThread.start();

        //Now Let's simulate another process writing to the file
        try(final Writer writer = new BufferedWriter(new FileWriter(file, true))) {

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    try {
                        writeText(writer, index);
                        // Sleeping 3 seconds ten times makes the writing process at least 30 seconds long
                        // meaning that at least once we will see the keepAlive event which is sent every 20 seconds
                        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
                    } catch (IOException e) {
                        fail("Error writing to file");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        fail("Error attempting to sleep thread");
                    }
                }));
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    fail("Error writing to file");
                }
            }

            final byte[] bytes = Files.readAllBytes(file.toPath());
            Logger.info(TailLogResourceTest.class,String.format("File content:%n%s",new String(bytes)) );
            assertTrue(bytes.length > 0);
        }finally {
            tailerThread.stopTailer();
        }

        assertFalse("We should have collected a bunch of write events. ",collectedEvents.isEmpty());

        Assert.assertTrue(collectedEvents.stream().anyMatch(t -> t._1.equals("success")));
        Assert.assertTrue(collectedEvents.stream().anyMatch(t -> t._1.equals("keepAlive")));

        collectedEvents.stream().filter(t -> t._1.equals("success")).forEach(t -> validateSuccessEvent(t._1, t._2));
        collectedEvents.stream().filter(t -> t._1.equals("keepAlive")).forEach(t -> validateKeepAliveEvent(t._1, t._2));
    }

    void validateSuccessEvent(final String name, final Map<String,?> data){
        assertEquals("success", name);
        final String lines = data.get("lines").toString();
        assertTrue(lines.contains(WRITTEN) || lines.contains(TAILING));
        Number pageId = (Number)data.get("pageId");
        assertTrue(pageId.intValue() > 0);
    }

    void validateKeepAliveEvent(final String name, final Map<String,?> data){
        assertEquals("keepAlive", name);
        final Boolean bool = (Boolean) data.get("keepAlive");
        assertTrue(bool);
    }

    // ==================== GET /v1/logs (listLogFiles) ====================

    /**
     * Given scenario: redirect {@code TAIL_LOG_LOG_FOLDER} to an isolated temp directory and
     *                 plant two matching {@code .log} files and one non-matching {@code .txt}
     *                 file inside it
     * Expected result: both {@code .log} files appear in the returned list (sorted alphabetically
     *                  and stripped of the base path), and the {@code .txt} file is filtered out
     *
     * <p>The override is fully restored in {@code finally} so the test never pollutes the real
     * dotsecure/logs directory or leaks state across tests.
     */
    @Test
    public void test_listLogFiles_filtersAndSortsAndStripsBasePath() throws Exception {
        final String originalLogFolder = Config.getStringProperty(
                "TAIL_LOG_LOG_FOLDER", "./dotsecure/logs/");

        final File tempLogFolder = Files.createTempDirectory("taillog-list-").toFile();
        tempLogFolder.deleteOnExit();

        final File firstMatch = plantLogFile(tempLogFolder, "aa-itlist.log");
        final File secondMatch = plantLogFile(tempLogFolder, "zz-itlist.log");
        final File notMatching = plantLogFile(tempLogFolder, "aa-itlist.txt");

        try {
            Config.setProperty("TAIL_LOG_LOG_FOLDER", tempLogFolder.getAbsolutePath());

            final ResponseEntityListStringView result =
                    resource.listLogFiles(createRequestForUser(adminUser), mockResponse);

            assertNotNull(result);
            final List<String> names = result.getEntity();
            assertNotNull(names);

            assertTrue("listing should contain the first matching .log file: " + names,
                    names.contains(firstMatch.getName()));
            assertTrue("listing should contain the second matching .log file: " + names,
                    names.contains(secondMatch.getName()));
            assertFalse("non-matching .txt file must be filtered out by TAIL_LOG_FILE_REGEX: " + names,
                    names.contains(notMatching.getName()));

            // base path is stripped — no absolute path leaks through, no leading separator
            final String basePath = tempLogFolder.getAbsolutePath();
            for (final String name : names) {
                assertFalse("listed name must not contain the absolute log folder path: " + name,
                        name.contains(basePath));
                assertFalse("listed name must not start with a separator: " + name,
                        name.startsWith(File.separator));
            }

            // sort order: aa- before zz- among our planted files
            assertTrue("aa-* entry must sort before zz-* entry: " + names,
                    names.indexOf(firstMatch.getName()) < names.indexOf(secondMatch.getName()));
        } finally {
            Config.setProperty("TAIL_LOG_LOG_FOLDER", originalLogFolder);
            FileUtils.deleteQuietly(tempLogFolder);
        }
    }

    @Test(expected = SecurityException.class)
    public void test_listLogFiles_asNonAdmin_throwsSecurity() {
        resource.listLogFiles(createRequestForUser(nonAdminUser), mockResponse);
    }

    private static HttpServletRequest createRequestForUser(final User user) {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()
        ).request();
        request.setAttribute(WebKeys.USER, user);
        return request;
    }

    private static File plantLogFile(final File folder, final String name) throws IOException {
        final File file = new File(folder, name);
        Files.writeString(file.toPath(), "test content");
        file.deleteOnExit();
        return file;
    }

}
