package com.dotcms.autoupdater;

import java.io.File;
import java.io.IOException;

public class PostProcess {

    private String distributionHome;
    private String dotcmsHome;

    public PostProcess ( String distributionHome, String dotcmsHome ) {
        this.distributionHome = distributionHome;
        this.dotcmsHome = dotcmsHome;
    }

    public boolean postProcess ( boolean clean ) {

        AntInvoker invoker = new AntInvoker( getDistributionHome() );
        try {
            if ( clean ) {
                boolean ret = invoker.runTask( "clean-plugins", getDistributionHome() + File.separator + UpdateAgent.FOLDER_HOME_BIN + File.separator + "ant" + File.separator + "build.xml" );
                if ( !ret ) {
                    return false;
                }
            }
            return invoker.runTask( "deploy-plugins", getDistributionHome() + File.separator + UpdateAgent.FOLDER_HOME_BIN + File.separator + "ant" + File.separator + "build.xml" );

        } catch ( IOException e ) {
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

}