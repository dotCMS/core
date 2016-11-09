package com.dotcms.listeners;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.jmx.DotMBean;
import com.dotmarketing.util.Logger;

import javax.management.*;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.lang.management.ManagementFactory;
import java.rmi.RMISecurityManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This Listener register the JMX MBeans when the context starts up.
 * @author jsanca
 */
public class RegisterMBeansListener implements ServletContextListener,
        HttpSessionListener, HttpSessionAttributeListener {

    private final List<ObjectName> names = new ArrayList<>();

    public RegisterMBeansListener() {
    }

    public void contextInitialized(ServletContextEvent sce) {

        if (Logger.isDebugEnabled(this.getClass())) {

            Logger.debug(this.getClass(), "Registering Mbeans...");
        }

        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final Set<DotMBean> mBeanSet  = this.getMbeans();
        ObjectName  objectName;

        try {

            for (DotMBean mbean : mBeanSet) {

                objectName = new ObjectName(mbean.getObjectName());
                server.registerMBean(mbean, objectName);
                this.names.add(objectName);

                if (Logger.isDebugEnabled(this.getClass())) {

                    Logger.debug(this.getClass(), "MBean registered: " + mbean.getObjectName());
                }
            }
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException |
                MBeanRegistrationException | NotCompliantMBeanException e) {

            Logger.error(this.getClass(), e.getMessage(), e);
        }
    }

    // Register here all the mbeans instances you want!
    protected Set<DotMBean> getMbeans () {

        final Set<DotMBean> mbeans = new HashSet<>();

        mbeans.add(DotConcurrentFactory.getInstance());

        return mbeans;
    }


    public void contextDestroyed(ServletContextEvent sce) {

        if (Logger.isDebugEnabled(this.getClass())) {

            Logger.debug(this.getClass(), "Unregistering Mbeans...");
        }

        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {

            for (ObjectName objectName : this.names) {

                server.unregisterMBean(objectName);
                if (Logger.isDebugEnabled(this.getClass())) {

                    Logger.debug(this.getClass(), "MBean registered: " + objectName);
                }
            }
        } catch (InstanceNotFoundException  |
                MBeanRegistrationException e) {

            Logger.error(this.getClass(), e.getMessage(), e);
        }
    }

    // -------------------------------------------------------
    // HttpSessionListener implementation
    // -------------------------------------------------------
    public void sessionCreated(HttpSessionEvent se) {
      /* Session is created. */
    }

    public void sessionDestroyed(HttpSessionEvent se) {
      /* Session is destroyed. */
    }

    // -------------------------------------------------------
    // HttpSessionAttributeListener implementation
    // -------------------------------------------------------

    public void attributeAdded(HttpSessionBindingEvent sbe) {
      /* This method is called when an attribute 
         is added to a session.
      */
    }

    public void attributeRemoved(HttpSessionBindingEvent sbe) {
      /* This method is called when an attribute
         is removed from a session.
      */
    }

    public void attributeReplaced(HttpSessionBindingEvent sbe) {
      /* This method is invoked when an attibute
         is replaced in a session.
      */
    }
}
