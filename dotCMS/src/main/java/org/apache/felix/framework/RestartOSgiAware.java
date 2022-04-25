package org.apache.felix.framework;

/**
 * This interface is useful to be aware on OSGI restart
 * @author jsanca
 */
public interface RestartOSgiAware {

    /**
     * Called when restart the osgi framework
     */
    void onRestartOsgi();
}
