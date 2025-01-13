package com.dotmarketing.servlets;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
//import javax.servlet.annotation.WebServlet;

/**
 * This servlet is used to check if the server should be shutdown on startup.
 * If the property shutdown-on-startup is set to true, the server will be shutdown
 * The loadOnStartup is set to the highest value to ensure it is run after all other servlets that are not
 * lazy loaded
 */
public class FinalStartupServlet extends HttpServlet {

    // init method
    @Override
    public void init(ServletConfig cfg) throws ServletException {
        super.init(cfg);
        Logger.info(this, "Starting FinalStartupServlet");
        if (Config.getBooleanProperty("shutdown-on-startup", false)) {
            Logger.info(this, "shutdown-on-startup is true, shutting down the server");
            System.exit(0);
        }
    }

    @Override
    public void destroy() {
        Logger.info(this, "FinalStartupServlet destroyed");
    }

}
