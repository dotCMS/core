package org.jboss.weld.environment.servlet;

import static org.jboss.weld.config.ConfigurationKey.BEAN_IDENTIFIER_INDEX_OPTIMIZATION;

import com.dotcms.jersey.DotWeldBootstrap;
import java.lang.reflect.Field;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.bootstrap.api.CDI11Bootstrap;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.configuration.spi.ExternalConfiguration;
import org.jboss.weld.configuration.spi.helpers.ExternalConfigurationBuilder;
import org.jboss.weld.environment.ContainerInstance;
import org.jboss.weld.environment.ContainerInstanceFactory;
import org.jboss.weld.environment.logging.CommonLogger;
import org.jboss.weld.environment.servlet.logging.WeldServletLogger;
import org.jboss.weld.environment.servlet.services.ServletResourceInjectionServices;
import org.jboss.weld.environment.util.DevelopmentMode;
import org.jboss.weld.environment.util.Reflections;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.manager.api.WeldManager;
import org.jboss.weld.module.web.el.WeldELContextListener;
import org.jboss.weld.resources.ManagerObjectFactory;
import org.jboss.weld.resources.WeldClassLoaderResourceLoader;

public class DotWeldServletLifecycle extends WeldServletLifecycle {

    private static final String CONTEXT_PARAM_DEV_MODE = "org.jboss.weld.development";

    private static final String JSP_FACTORY_CLASS_NAME = "javax.servlet.jsp.JspFactory";

    private static final String EXPRESSION_FACTORY_NAME = "org.jboss.weld.el.ExpressionFactory";

    public DotWeldServletLifecycle() {
        super();
    }

    public boolean initialize(ServletContext context) {

         setDevModeEnabled(Boolean.parseBoolean(context.getInitParameter(CONTEXT_PARAM_DEV_MODE)));

        WeldManager manager = (WeldManager) context.getAttribute(BEAN_MANAGER_ATTRIBUTE_NAME);
        if (manager != null) {
            setBootstrapNeeded(false);
            String contextId = BeanManagerProxy.unwrap(manager).getContextId();
            context.setInitParameter(org.jboss.weld.Container.CONTEXT_ID_KEY, contextId);
        } else {
            Object container = context.getAttribute(Listener.CONTAINER_ATTRIBUTE_NAME);
            if (container instanceof ContainerInstanceFactory) {
                ContainerInstanceFactory factory = (ContainerInstanceFactory) container;
                // start the container
                ContainerInstance containerInstance = factory.initialize();
                container = containerInstance;
                // we are in charge of shutdown also
                setShutdownAction(containerInstance::shutdown);
            }
            if (container instanceof ContainerInstance) {
                // the container instance was either passed to us directly or was created in the block above
                ContainerInstance containerInstance = (ContainerInstance) container;
                manager = BeanManagerProxy.unwrap(containerInstance.getBeanManager());
                context.setInitParameter(org.jboss.weld.Container.CONTEXT_ID_KEY, containerInstance.getId());
                setBootstrapNeeded(false);
            }
        }

        //Replace CDI bootstrap with ours
        final CDI11Bootstrap bootstrap = new DotWeldBootstrap();
        if (isBootstrapNeeded()) {
            final CDI11Deployment deployment = createDeployment(context, bootstrap);

            deployment.getServices().add(ExternalConfiguration.class,
                    new ExternalConfigurationBuilder().add(BEAN_IDENTIFIER_INDEX_OPTIMIZATION.get(), Boolean.FALSE.toString()).build());

            if (deployment.getBeanDeploymentArchives().isEmpty()) {
                // Skip initialization - there is no bean archive in the deployment
                CommonLogger.LOG.initSkippedNoBeanArchiveFound();
                return false;
            }

            ResourceInjectionServices resourceInjectionServices = new ServletResourceInjectionServices() {
            };
            try {
                for (BeanDeploymentArchive archive : deployment.getBeanDeploymentArchives()) {
                    archive.getServices().add(ResourceInjectionServices.class, resourceInjectionServices);
                }
            } catch (NoClassDefFoundError e) {
                // Support GAE
                WeldServletLogger.LOG.resourceInjectionNotAvailable();
            }

            String id = context.getInitParameter(org.jboss.weld.Container.CONTEXT_ID_KEY);
            if (id != null) {
                bootstrap.startContainer(id, Environments.SERVLET, deployment);
            } else {
                bootstrap.startContainer(Environments.SERVLET, deployment);
            }
            bootstrap.startInitialization();

            /*
             * Determine the BeanManager used for example for EL resolution - this should work fine as all bean archives share the same classloader. The only
             * difference this can make is per-BDA (CDI 1.0 style) enablement of alternatives, interceptors and decorators. Nothing we can do about that.
             *
             * First try to find the bean archive for WEB-INF/classes. If not found, take the first one available.
             */
            for (BeanDeploymentArchive bda : deployment.getBeanDeploymentArchives()) {
                if (bda.getId().contains(ManagerObjectFactory.WEB_INF_CLASSES_FILE_PATH) || bda.getId().contains(ManagerObjectFactory.WEB_INF_CLASSES)) {
                    manager = bootstrap.getManager(bda);
                    break;
                }
            }
            if (manager == null) {
                manager = bootstrap.getManager(deployment.getBeanDeploymentArchives().iterator().next());
            }

            // Push the manager into the servlet context so we can access in JSF
            context.setAttribute(BEAN_MANAGER_ATTRIBUTE_NAME, manager);
        }

        ContainerContext containerContext = new ContainerContext(context, manager);
        StringBuilder dump = new StringBuilder();
        Container container = findContainer(containerContext, dump);
        if (container == null) {
            WeldServletLogger.LOG.noSupportedServletContainerDetected();
            WeldServletLogger.LOG.debugv("Exception dump from Container lookup: {0}", dump);
        } else {
            container.initialize(containerContext);
            setContainer(container);
        }

        if (Reflections.isClassLoadable(WeldClassLoaderResourceLoader.INSTANCE, JSP_FACTORY_CLASS_NAME) && JspFactory.getDefaultFactory() != null) {
            JspApplicationContext jspApplicationContext = JspFactory.getDefaultFactory().getJspApplicationContext(context);

            // Register the ELResolver with JSP
            jspApplicationContext.addELResolver(manager.getELResolver());

            // Register ELContextListener with JSP
            try {
                jspApplicationContext.addELContextListener(new WeldELContextListener());
            } catch (Exception e) {
                throw WeldServletLogger.LOG.errorLoadingWeldELContextListener(e);
            }

            // Push the wrapped expression factory into the servlet context so that Tomcat or Jetty can hook it in using a container code
            context.setAttribute(EXPRESSION_FACTORY_NAME, manager.wrapExpressionFactory(jspApplicationContext.getExpressionFactory()));
        }

        if (isBootstrapNeeded()) {

            bootstrap.deployBeans().validateBeans().endInitialization();

            if (isDevModeEnabled()) {
                FilterRegistration.Dynamic filterDynamic = context.addFilter("Weld Probe Filter", DevelopmentMode.PROBE_FILTER_CLASS_NAME);
                filterDynamic.addMappingForUrlPatterns(
                        EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE), true, "/*");
            }
            setShutdownAction(bootstrap::shutdown);
        }
        return true;
    }

    public void setBootstrapNeeded(boolean bootstrapNeeded) {
        try {
            Field bootstrapField = this.getClass().getSuperclass().getDeclaredField("isBootstrapNeeded");
            bootstrapField.setAccessible(true);
            bootstrapField.set(this, bootstrapNeeded);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Could not find 'isBootstrapNeeded' field in parent class", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access 'isBootstrapNeeded' field in parent class", e);
        }
    }

    public boolean isBootstrapNeeded() {
        try {
            Field bootstrapField = this.getClass().getSuperclass().getDeclaredField("isBootstrapNeeded");
            bootstrapField.setAccessible(true);
            return (boolean) bootstrapField.get(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not access 'isBootstrapNeeded' field", e);
        }
    }

    public void setDevModeEnabled(boolean devModeEnabled) {
        try {
            Field devModeField = this.getClass().getSuperclass().getDeclaredField("isDevModeEnabled");
            devModeField.setAccessible(true);
            devModeField.set(this, devModeEnabled);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Could not find 'isDevModeEnabled' field in parent class", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access 'isDevModeEnabled' field in parent class", e);
        }
    }

    public boolean isDevModeEnabled() {
        try {
            Field devModeField = this.getClass().getSuperclass().getDeclaredField("isDevModeEnabled");
            devModeField.setAccessible(true);
            return (boolean) devModeField.get(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not access 'isDevModeEnabled' field", e);
        }
    }

    public void setContainer(Container container) {
        try {
            Field containerField = this.getClass().getSuperclass().getDeclaredField("container");
            containerField.setAccessible(true);
            containerField.set(this, container);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Could not find 'container' field in parent class", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access 'container' field in parent class", e);
        }
    }

    public void setShutdownAction(Runnable shutdownAction) {
        try {
            Field shutdownActionField = this.getClass().getSuperclass().getDeclaredField("shutdownAction");
            shutdownActionField.setAccessible(true);
            shutdownActionField.set(this, shutdownAction);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Could not find 'shutdownAction' field in parent class", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access 'shutdownAction' field in parent class", e);
        }
    }

}
