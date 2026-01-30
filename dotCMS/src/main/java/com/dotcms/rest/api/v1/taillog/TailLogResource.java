package com.dotcms.rest.api.v1.taillog;

import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
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
import org.apache.commons.io.input.TailerListenerAdapter;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.dotmarketing.util.FileUtil.isValidFilePath;
import static com.dotmarketing.util.FileUtil.sanitizeFilePath;

/**
 * This resource provides the endpoint used by the LogViewer functionality to display backend server logs
 * @author nollymarlonga
 */
@Path("/v1/logs")
@Tag(name = "TailLog")
public class TailLogResource {

    public static final int LINES_PER_PAGE = Config.getIntProperty("TAIL_LOG_LINES_PER_PAGE",10);

    //This is in seconds
    public static final int KEEP_ALIVE_EVENT_INTERVAL = Config.getIntProperty("KEEP_ALIVE_EVENT_INTERVAL",20);

    @GET
    @Path("/{fileName:.+}/_tail")
    @JSONP
    @NoCache
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public final EventOutput getLogs(@Context final HttpServletRequest request,
            @PathParam("fileName") final String fileName, @QueryParam("linesBack") final int linesBack) throws IOException {

        final InitDataObject initData =
                new WebResource.InitBuilder(new WebResource())
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, new EmptyHttpResponse())
                        .rejectWhenNoUser(true)
                        .init();

        if(fileName.trim().isEmpty()) {
            return sendError("Empty File name param");
        }

//This prevents any evil attack attempt allowing for paths including subfolders
        if(!isValidFilePath(fileName)){
            return sendError("Invalid File name param");
        }

        final String sanitizedFileName = sanitizeFilePath(fileName);
        String tailLogLofFolder = Config.getStringProperty("TAIL_LOG_LOG_FOLDER", "./dotsecure/logs/");
        if (!tailLogLofFolder.endsWith(File.separator)) {
            tailLogLofFolder = tailLogLofFolder + File.separator;
        }

        final File logFolder 	= new File(FileUtil.getAbsolutlePath(tailLogLofFolder));
        final File logFile 	= new File(FileUtil.getAbsolutlePath(tailLogLofFolder + sanitizedFileName));


        // if the logFile is outside the logFolder, die
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
            return sendError("File name does not match a valid file pattern. ");
        }

        final MyTailerListener listener = new MyTailerListener();


        final MyTailerThread thread = new MyTailerThread(logFile, listener, new EventOutput(),
                linesBack);

        String name = null;

        //Finds a new thread to read the log
        for (int i = 0; i < 1000; i++) {
            name = "Log-Tailer" + i + ":" + sanitizedFileName;
            Thread t = ThreadUtils.getThread(name);
            if (t == null) {
                Logger.warn(TailLogResource.class," TailLog Thread available with name: " +name);
                break;
            }
            if (i > 100) {
               return sendError("Too many Logger threads");
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
                Map.of("failure", errorMessage));
        final OutboundEvent event = eventBuilder.build();
        try {
            eventOutput.write(event);
        } catch (Exception e1) {
            throw new DotRuntimeException(e1);
        }

        return eventOutput;
    }


    static class MyTailerListener extends TailerListenerAdapter {

       private final StringBuilder out = new StringBuilder();

        public void handle(final String line) {
            synchronized (this) {
                out.append(UtilMethods.xmlEscape(line)).append("<br/>");
            }
        }

        String getThenDispose() {
            synchronized (this) {
                try {
                    return out.toString();
                } finally {
                    out.delete(0, out.length());
                }
            }
        }

    }

    static class MyTailerThread extends Thread {

        final Tailer tailer;
        final EventOutput eventOutput;
        final MyTailerListener listener;
        final String fileName;

        @Override
        public final void run() {

            final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
            try {
                long timeMark = 0;
                int logNumber = 0;
                int count = 1;
                int pageNumber = 1;
                while (!eventOutput.isClosed()) {
                    final String write = listener.getThenDispose();
                    if (!write.isEmpty()) {
                        final String prepWrite = String.format(
                                "<p class=\"log page%d\" data-page=\"%d\" data-logNumber=\"%d\" style=\"margin:0\">%s</p>",
                                pageNumber, pageNumber, logNumber, write);

                        eventBuilder.name("success");
                        eventBuilder.data(Map.class,
                                Map.of("lines", prepWrite, "pageId", pageNumber));
                        eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE);
                        final OutboundEvent event = eventBuilder.build();
                        eventOutput.write(event);

                        count++;
                        logNumber++;
                        if (count % (LINES_PER_PAGE + 1) == 0) {
                            pageNumber++;
                            count = 1;
                        }
                    } else {
                        if(System.currentTimeMillis() > timeMark + TimeUnit.SECONDS.toMillis(KEEP_ALIVE_EVENT_INTERVAL)){
                            Logger.debug(this.getClass(), String.format(" Thread [%s] is sending keepAlive event for file [%s] ", getName(), fileName));
                            eventBuilder.name("keepAlive");
                            eventBuilder.data(Map.class,
                                    Map.of("keepAlive", true ));
                            eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE);
                            final OutboundEvent event = eventBuilder.build();
                            eventOutput.write(event);
                            timeMark = System.currentTimeMillis();
                        }
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception ex) {
                Logger.warn(this.getClass(), String.format(" Thread [%s] has stopped listening log events for file [%s] with reason [%s] ", getName(), fileName, ex.getMessage()));
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
            listener.handle("Tailing info from file " + fileName);
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

        public EventOutput getEventOutput(){
            return eventOutput;
        }

    }
}
