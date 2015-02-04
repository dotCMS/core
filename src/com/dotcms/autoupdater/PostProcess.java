package com.dotcms.autoupdater;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class PostProcess {

    private String distributionHome;
    private String dotcmsHome;

    public PostProcess ( String distributionHome, String dotcmsHome ) {
        this.distributionHome = distributionHome;
        this.dotcmsHome = dotcmsHome;
    }

    public boolean postProcess () {

        try {
            //Verify the OS
            boolean isWindows = System.getProperty( "os.name" ).toLowerCase().startsWith( "windows" );

            //Depending on the OS we will try to execute the deploy-plugins.bat or the deploy-plugins.sh script
            String filePath = getDistributionHome() + File.separator + UpdateAgent.FOLDER_HOME_BIN + File.separator;
            if ( isWindows ) {
                filePath += "deploy-plugins.bat";
            } else {
                filePath += "deploy-plugins.sh";
            }
            return execShellCmd( filePath, isWindows );

        } catch ( Exception e ) {
            UpdateAgent.logger.fatal( "IOException: " + e.getMessage(), e );
        }
        return false;
    }

    public boolean checkRequisites () throws UpdateException {
        AntInvoker invoker = new AntInvoker( getDistributionHome() );
        return invoker.checkRequisites();
    }

    public String getDistributionHome () {
        return distributionHome;
    }

    public void setDistributionHome ( String distributionHome ) {
        this.distributionHome = distributionHome;
    }

    public String getDotcmsHome () {
        return dotcmsHome;
    }

    public void setDotcmsHome ( String dotcmsHome ) {
        this.dotcmsHome = dotcmsHome;
    }

    /**
     * Executes a given script file
     *
     * @param filePath
     * @param isWindows
     * @return
     */
    public static Boolean execShellCmd ( String filePath, Boolean isWindows ) {

        try {
            Runtime runtime = Runtime.getRuntime();

            //If Unix we need the script with the proper permissions in order to be able to execute it
            if ( !isWindows ) {
                runtime.exec( "chmod 755 " + filePath ).waitFor();
            }

            Process process = runtime.exec( filePath );//Execute the script
            process.waitFor();//Causes the current thread to wait, if necessary, until the process represented by this Process object has terminated.

            if ( UpdateAgent.isDebug ) {
                BufferedReader buf = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
                String line;
                while ( (line = buf.readLine()) != null ) {
                    UpdateAgent.logger.info( line ); //If something to log
                }
            }

            return true;
        } catch ( Exception e ) {
            String genericErrorMessage = Messages.getString( "UpdateAgent.text.use.verbose", UpdateAgent.logFile );
            UpdateAgent.logger.fatal( "Unable to deploy plugins. " + genericErrorMessage, e );

            return false;
        }
    }

}