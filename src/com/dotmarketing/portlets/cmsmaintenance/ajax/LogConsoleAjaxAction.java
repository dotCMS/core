package com.dotmarketing.portlets.cmsmaintenance.ajax;

import com.dotmarketing.logConsole.model.LogMapper;
import com.dotmarketing.logConsole.model.LogMapperRow;
import com.dotmarketing.util.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by Jonathan Gamba.
 * Date: 5/18/12
 */
public class LogConsoleAjaxAction extends IndexAjaxAction {

    public static final String CONTENT_JSON = "application/json";

    /**
     * Method that will get the current logs list and will send them to the client in a JSON format
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void getLogs ( HttpServletRequest request, HttpServletResponse response ) throws JSONException, IOException {

        //Preparing the log list json to send it to the client
        JSONObject jsonResponse = prepareAndResponseLogList( response );

        //Sending the json response
        prepareResponse( jsonResponse.toString(), CONTENT_JSON, response );
    }

    /**
     * Method that will enable/disable a list of given logs
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void enabledDisabledLogs ( HttpServletRequest request, HttpServletResponse response ) throws JSONException, IOException {

        JSONObject jsonResponse = new JSONObject();

        try {

            //Getting the names of the selected logs
            String[] selectedLogs = null;
            String selectedLogsParam = getURIParams().get( "selection" );
            if ( selectedLogsParam != null ) {
                selectedLogs = selectedLogsParam.split( "," );
            }

            if ( selectedLogs != null ) {

                //Getting our current logs
                Collection<LogMapperRow> currentLogs = LogMapper.getInstance().getLogList();

                for ( LogMapperRow logMapperRow : currentLogs ) {
                    for ( String selectedLog : selectedLogs ) {

                        //Compare to see if this log was selected in the UI by the client
                        if ( logMapperRow.getLog_name().equals( selectedLog ) ) {

                            if ( logMapperRow.getEnabled() == 1 ) {
                                logMapperRow.setEnabled( 0 );
                            } else {
                                logMapperRow.setEnabled( 1 );
                            }
                        }
                    }
                }

                //Update the modified logs
                LogMapper.getInstance().updateLogsList();
            }

            //And finally preparing the log list json to send it to the client with the modified values
            jsonResponse = prepareAndResponseLogList( response );

        } catch ( Exception e ) {

            jsonResponse.put( "response", "error" );
            jsonResponse.put( "message", "Error updating logs." );
            Logger.error( LogConsoleAjaxAction.class, "Error updating logs.", e );
        }

        //Sending the json response
        prepareResponse( jsonResponse.toString(), CONTENT_JSON, response );
    }

    /**
     * Method that will get the current logs list and will create the required JSON to send them to the client
     *
     * @param response
     * @throws JSONException
     * @throws IOException
     */
    private JSONObject prepareAndResponseLogList ( HttpServletResponse response ) throws JSONException, IOException {

        JSONObject jsonResponse = new JSONObject();

        try {
            //Getting our current logs
            Collection<LogMapperRow> logList = LogMapper.getInstance().getLogList();

            //Preparing a json response
            JSONArray logsJSONArray = new JSONArray();

            for ( LogMapperRow logMapperRow : logList ) {

                JSONObject jsonLogMapperRow = new JSONObject();
                jsonLogMapperRow.put( "name", logMapperRow.getLog_name() );
                jsonLogMapperRow.put( "enabled", logMapperRow.getEnabled() );
                jsonLogMapperRow.put( "description", logMapperRow.getDescription() );

                //Add it to our json array
                logsJSONArray.put( jsonLogMapperRow );
            }

            //Preparing the json response
            jsonResponse.put( "logs", logsJSONArray );
            jsonResponse.put( "response", "sucess" );
        } catch ( Exception e ) {
            jsonResponse.put( "response", "error" );
            jsonResponse.put( "message", "Error retrieving logs." );
            Logger.error( LogConsoleAjaxAction.class, "Error retrieving logs.", e );
        }

        return jsonResponse;
    }

    /**
     * Prepare the response for the AJAX call
     *
     * @param responseData -
     * @param contentType  -
     * @param response     -
     * @throws java.io.IOException -
     */
    protected void prepareResponse ( String responseData, String contentType, HttpServletResponse response ) throws IOException {

        response.setContentType( contentType );

        ServletOutputStream sos = response.getOutputStream();
        sos.write( responseData.getBytes() );
        sos.flush();
        sos.close();

        response.flushBuffer();
    }

}