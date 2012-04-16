package com.dotmarketing.plugin.ant;

import com.dotmarketing.plugin.util.PluginFileMerger;
import com.dotmarketing.plugin.util.PluginRoot;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * @author Andres Olarte
 * @since 1.6.5c
 */
public class UndeployTask extends Task {

    private String root;
    private String plugins;

    @Override
    public void execute () throws BuildException {

        //Init log4j to see the messages in ant's output
        Logger logRoot = Logger.getRootLogger();
        if ( !logRoot.getAllAppenders().hasMoreElements() ) {
            logRoot.addAppender( new ConsoleAppender( new PatternLayout( "%m%n" ) ) );
        }
        new PluginRoot( root, plugins ).undeploy();//Plugin that will allow any file to be overridden or added.
        new PluginFileMerger().undeploy( root, plugins );
    }

    /**
     * Set the root of the web app (Servlet context)
     *
     * @param root The root of the web app
     */
    public synchronized void setRoot ( String root ) {
        this.root = root;
    }

    /**
     * Set the directory where the plugins live
     *
     * @param plugins The directory where the plugins live
     */
    public synchronized void setPlugins ( String plugins ) {
        this.plugins = plugins;
    }

}