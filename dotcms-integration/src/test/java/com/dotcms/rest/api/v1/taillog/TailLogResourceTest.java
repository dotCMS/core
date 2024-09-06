package com.dotcms.rest.api.v1.taillog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.rest.api.v1.taillog.TailLogResource.MyTailerListener;
import com.dotcms.rest.api.v1.taillog.TailLogResource.MyTailerThread;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Logger;
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
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TailLogResourceTest {

    static final String TAILING = "Tailing info from file";
    static final String WRITTEN = "New Line written With Number";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
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

}
