package com.dotcms.rest.api.v1.taillog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.rest.api.v1.taillog.TailLogResource.MyTailerListener;
import com.dotcms.rest.api.v1.taillog.TailLogResource.MyTailerThread;
import com.dotcms.util.IntegrationTestInitService;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.junit.BeforeClass;
import org.junit.Test;

public class TailLogResourceTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
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
        final File file = File.createTempFile("tailLog", ".txt");
        final TailLogResource resource = new TailLogResource();
        final MyTailerListener listener = new MyTailerListener();
        final EventOutput eventOutput = new TailerTestEventOutput();
        final MyTailerThread tailerThread = new MyTailerThread(file, listener, eventOutput, 300);
        tailerThread.start();
        try {
            //Give some time to the tailerThread to read the file
            Thread.sleep(500);
            Object outputData = ((TailerTestEventOutput) eventOutput).getTestOutputData();
            String outputName = ((TailerTestEventOutput) eventOutput).getTestOutputName();

            assertEquals("success", outputName);
            assertTrue(outputData instanceof Map);
            assertTrue(((Map)outputData).containsKey("lines"));
            assertTrue(((Map)outputData).get("lines").toString().contains("Tailing info from file"));
        } finally {
            //we force the event execution and stop the thread
            eventOutput.close();
            tailerThread.stopTailer();
            //Give some time to the tailerThread to stop
            Thread.sleep(2000);

            if (tailerThread.isAlive()){
                fail("TailerThread should have stopped");
            }
        }
    }

    static class TailerTestEventOutput extends EventOutput{

        private Object testOutputData;
        private String testOutputName;

        TailerTestEventOutput(){
            super();
        }
        public void write(final OutboundEvent outboundEvent) throws IOException {
            testOutputData = outboundEvent.getData();
            testOutputName = outboundEvent.getName();
        }

        public Object getTestOutputData() {
            return testOutputData;
        }

        public String getTestOutputName(){
            return testOutputName;
        }
    }

}
