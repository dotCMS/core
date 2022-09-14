package com.dotcms.business.bytebuddy;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ByteBuddyLogListener implements AgentBuilder.Listener {
    private Logger LOGGER = LogManager.getLogger(ByteBuddyLogListener.class);
    @Override
    public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        LOGGER.debug("Discovered: " + typeName+" loaded: "+loaded);
    }

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
        LOGGER.debug("Transformed "+typeDescription.getName() +" loaded: "+loaded);
    }

    @Override
    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
        LOGGER.debug("Ignored "+typeDescription.getName()+" loaded: "+loaded);
    }

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
        LOGGER.debug( "ByteBuddy error with class "+typeName, throwable);
    }

    @Override
    public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        LOGGER.debug( "Completed "+typeName);
    }
}