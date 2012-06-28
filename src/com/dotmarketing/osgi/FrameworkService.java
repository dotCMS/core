package com.dotmarketing.osgi;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;

import javax.servlet.ServletContext;
import java.util.*;

/**
 * Created by Jonathan Gamba
 * Date: 6/27/12
 */
public final class FrameworkService {

    private final ServletContext context;
    private Felix felix;

    public FrameworkService ( ServletContext context ) {
        this.context = context;
    }

    public void start () {
        try {
            doStart();
        } catch ( Exception e ) {
            log( "Failed to start framework", e );
        }
    }

    public void stop () {
        try {
            doStop();
        } catch ( Exception e ) {
            log( "Error stopping framework", e );
        }
    }

    private void doStart () throws Exception {

        Felix tmp = new Felix( createConfig() );
        tmp.start();
        this.felix = tmp;
        log( "OSGi framework started", null );
    }

    private void doStop () throws Exception {

        if ( this.felix != null ) {
            this.felix.stop();
        }

        log( "OSGi framework stopped", null );
    }

    private Map<String, Object> createConfig () throws Exception {

        Properties properties = new Properties();

        HashMap<String, Object> map = new HashMap<String, Object>();
        Iterator<String> it = Config.getKeys();
        while ( it.hasNext() ) {

            String key = it.next();
            if ( key == null ) continue;
            if ( key.startsWith( "felix." ) ) {
                map.put( key.substring( 6 ), Config.getStringProperty( key ) );
                properties.put( key.substring( 6 ), Config.getStringProperty( key ) );
                Logger.info( this, "Loading property  " + key.substring( 6 ) + "=" + Config.getStringProperty( key ) );
            }
        }

        map.put( FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, Arrays.asList( new ProvisionActivator( this.context ) ) );
        return map;
    }

    private void log ( String message, Throwable cause ) {
        this.context.log( message, cause );
    }

}