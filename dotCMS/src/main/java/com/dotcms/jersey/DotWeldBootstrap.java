package com.dotcms.jersey;

import java.lang.reflect.Field;
import org.jboss.weld.bootstrap.DotWeldStartup;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.WeldStartup;

public class DotWeldBootstrap extends WeldBootstrap {

    public DotWeldBootstrap() {
        super();
        setWeldStartup(new DotWeldStartup());
    }

    /**
     * Sets the private weldStartup field using reflection
     * Follows standard setter naming convention
     *
     * @param weldStartup the WeldStartup instance to set
     */
    public void setWeldStartup(WeldStartup weldStartup) {
        try {
            Field weldStartupField = WeldBootstrap.class.getDeclaredField("weldStartup");
            weldStartupField.setAccessible(true);
            weldStartupField.set(this, weldStartup);
        } catch (Exception e) {
            throw new RuntimeException("Could not set weldStartup field", e);
        }
    }

}
