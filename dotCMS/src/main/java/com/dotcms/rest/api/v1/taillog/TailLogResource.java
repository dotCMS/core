package com.dotcms.rest.api.v1.taillog;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.repackage.org.apache.commons.io.input.TailerListenerAdapter;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.util.CloseUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.servlets.taillog.Tailer;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.ThreadUtils;
import com.dotmarketing.util.UtilMethods;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.JSONP;

/**
 * This resource provides the endpoint used by the LogViewer functionality to display backend server logs
 * @author nollymarlonga
 */
@Path("/v1/tailLog")
@Tag(name = "TailLog")
public class TailLogResource {

    @GET
    @Path("/{fileName}")
    @JSONP
    @NoCache
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public final EventOutput getLogs(@Context final HttpServletRequest request,
            @PathParam("fileName") final String fileName, @PathParam("linesBack") final int linesBack) throws IOException {

        new WebResource().init(null, request, new EmptyHttpResponse(), true, null);


        if(fileName.trim().isEmpty()) {
            return sendError("File should not be empty");
        }


        final String sanitizedFileName = FileUtil.sanitizeFileName(fileName);
        String tailLogLofFolder = Config.getStringProperty("TAIL_LOG_LOG_FOLDER", "./dotsecure/logs/");
        if (!tailLogLofFolder.endsWith(File.separator)) {
            tailLogLofFolder = tailLogLofFolder + File.separator;
        }

        final File logFolder 	= new File(FileUtil.getAbsolutlePath(tailLogLofFolder));
        final File logFile 	= new File(FileUtil.getAbsolutlePath(tailLogLofFolder + sanitizedFileName));


        // if the logFile is outside of the logFolder, die
        if ( !logFolder.exists() || !logFolder.canRead()
                ||   !logFile.getCanonicalPath().startsWith(logFolder.getCanonicalPath())) {

            final String errorMessage = "Invalid File request:" + logFile.getCanonicalPath() + " from:" +request.getRemoteHost();
            SecurityLogger.logInfo(
                    TailLogResource.class,  errorMessage);
            return sendError(errorMessage);
        }

        Logger.info(this.getClass(), "Requested logFile:" + logFile.getCanonicalPath());

        final String regex = Config.getStringProperty("TAIL_LOG_FILE_REGEX", ".*\\.log$|.*\\.out$");


        if(!Pattern.compile(regex).matcher(sanitizedFileName).matches()){
            sendError("Error ");
        }

        final MyTailerListener listener = new MyTailerListener();


        final MyTailerThread thread = new MyTailerThread(logFile, listener, new EventOutput(), linesBack);

        String name = null;

        //Finds a new thread to read the log
        for (int i = 0; i < 1000; i++) {
            name = "LogTailer" + i + ":" + sanitizedFileName;
            Thread t = ThreadUtils.getThread(name);
            if (t == null) {
                break;
            }
            if (i > 100) {
                sendError("Too many Logger threads");
            }
        }

        thread.setName(name);

        thread.start();

        return thread.getEventOutput();
    }

    private EventOutput sendError(final String errorMessage){

        final EventOutput eventOutput = new EventOutput();

        final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
        eventBuilder.name("failure");
        eventBuilder.data(Map.class,
                map("failure", errorMessage));
        final OutboundEvent event = eventBuilder.build();
        try {
            eventOutput.write(event);
        } catch (Exception e1) {
            throw new DotRuntimeException(e1);
        }

        return eventOutput;
    }


    class MyTailerListener extends TailerListenerAdapter {

        StringBuffer out = new StringBuffer();


        public void handle(String line) {
            getOut().append(UtilMethods.xmlEscape(line) + "<br />");
        }

        StringBuffer getOut() {
            return getOut(false);
        }

        StringBuffer getOut(boolean refresh) {
            synchronized (this) {
                if (refresh) {
                    StringBuffer s = new StringBuffer().append(out.toString());
                    this.out = new StringBuffer();
                    return s;
                }
                return out;
            }
        }

    }

    class MyTailerThread extends Thread {

        Tailer tailer;
        EventOutput eventOutput;
        MyTailerListener listener;
        String fileName;

        @Override
        public final void run() {

            final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
            try {
                while (!eventOutput.isClosed()) {
                    final String write = listener.getOut(true).toString();
                    if (write != null && write.length() > 0) {
                        eventBuilder.name("success");
                        eventBuilder.data(Map.class,
                                map("lines", write));
                        eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE);
                        final OutboundEvent event = eventBuilder.build();
                        eventOutput.write(event);
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception ex) {
                Logger.warn(this.getClass(), "Stopping listening log events for " + fileName + "Reason: " + ex.getMessage());
            } finally {
                stopTailer();
                CloseUtils.closeQuietly(eventOutput);
            }
        }

        public MyTailerThread(final File logFile, final MyTailerListener myTailerListener, final EventOutput myEventOutput, final int linesBack) {
            final int linesNumber = linesBack <= 0? 5000: linesBack;
            final long startPosition = ((logFile.length() - linesNumber) < 0) ? 0 : logFile.length() - linesNumber;
            fileName = logFile.getName();
            listener = myTailerListener;
            listener.handle("Tailing " + linesNumber + " lines from file " + fileName);
            listener.handle("----------------------------- ");
            tailer = Tailer.create(logFile, listener, 1000);
            tailer.setStartPosition(startPosition);
            eventOutput = myEventOutput;
        }


        public void stopTailer() {
            if (tailer != null) {
                tailer.stop();
            }
        }

        public void setTailer(Tailer tailer) {
            this.tailer = tailer;
        }

        public EventOutput getEventOutput(){
            return eventOutput;
        }

    }
}
