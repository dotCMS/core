package org.jboss.weld.bootstrap;

import static org.jboss.weld.config.ConfigurationKey.ROLLING_UPGRADES_ID_DELIMITER;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.Container;
import org.jboss.weld.ContainerState;
import org.jboss.weld.bean.proxy.util.WeldDefaultProxyServices;
import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.ServiceRegistries;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.bootstrap.spi.helpers.MetadataImpl;
import org.jboss.weld.config.ConfigurationKey;
import org.jboss.weld.config.ConfigurationKey.UnusedBeans;
import org.jboss.weld.config.WeldConfiguration;
import org.jboss.weld.logging.BootstrapLogger;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.BeanManagerLookupService;
import org.jboss.weld.metadata.TypeStore;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.resources.DefaultResourceLoader;
import org.jboss.weld.resources.ReflectionCache;
import org.jboss.weld.resources.ReflectionCacheFactory;
import org.jboss.weld.resources.SharedObjectCache;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.security.NoopSecurityServices;
import org.jboss.weld.security.spi.SecurityServices;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.jboss.weld.util.collections.Iterables;

import java.lang.reflect.Field;

public class DotWeldStartup extends WeldStartup {


    public WeldRuntime startContainer(String contextId, Environment environment, Deployment deployment) {
        if (deployment == null) {
            throw BootstrapLogger.LOG.deploymentRequired();
        }
        final Tracker tracker = getTracker();
        tracker.start(Tracker.OP_BOOTSTRAP);
        tracker.start(Tracker.OP_START_CONTAINER);
        checkApiVersion();

        final ServiceRegistry registry = deployment.getServices();

        // initiate part of registry in order to allow access to WeldConfiguration
        new AdditionalServiceLoader(deployment).loadAdditionalServices(registry);

        // Resource Loader has to be loaded prior to WeldConfiguration
        if (!registry.contains(ResourceLoader.class)) {
            registry.add(ResourceLoader.class, DefaultResourceLoader.INSTANCE);
        }

        WeldConfiguration configuration = new WeldConfiguration(registry, deployment);
        registry.add(WeldConfiguration.class, configuration);

        String finalContextId = BeanDeployments.getFinalId(contextId,
                registry.get(WeldConfiguration.class).getStringProperty(ROLLING_UPGRADES_ID_DELIMITER));
        setContextId(finalContextId);
        setDeployment(deployment);
        setEnvironment(environment);


        if (getExtensions() == null) {
            setExtensions(deployment.getExtensions());
        }
        // Add extension to register built-in components
        getExtensions().add(MetadataImpl.from(new WeldExtension()));

        // Additional Weld extensions
        String vetoTypeRegex = configuration.getStringProperty(ConfigurationKey.VETO_TYPES_WITHOUT_BEAN_DEFINING_ANNOTATION);
        if (!vetoTypeRegex.isEmpty()) {
            getExtensions().add(MetadataImpl.from(new WeldVetoExtension(vetoTypeRegex)));
        }
        if (UnusedBeans.isEnabled(configuration)) {
            getExtensions().add(MetadataImpl.from(new WeldUnusedMetadataExtension()));
        }

        // Finish the rest of registry init, setupInitialServices() requires already changed finalContextId
        tracker.start(Tracker.OP_INIT_SERVICES);
        setupInitialServices();
        registry.addAll(getInitialServices().entrySet());

        if (!registry.contains(ProxyServices.class)) {
            // add our own default impl that supports class defining
            registry.add(ProxyServices.class, new WeldDefaultProxyServices());
        }
        // all implementations of ProxyServices need to support class defining
        ProxyServices proxyServices = registry.get(ProxyServices.class);
        if (!proxyServices.supportsClassDefining()) {
            throw BootstrapLogger.LOG.proxyServicesWithoutClassDefining(proxyServices.getClass().getName());
        }
        // if we use our own ProxyServices impl, we need to crack open CL for JDK 8 impl
        // note that this is no-op call for JDK 11+
        if (proxyServices instanceof WeldDefaultProxyServices) {
            WeldDefaultProxyServices.makeClassLoaderMethodsAccessible();
        }
        if (!registry.contains(SecurityServices.class)) {
            registry.add(SecurityServices.class, NoopSecurityServices.INSTANCE);
        }

        addImplementationServices(registry);
        tracker.end();

        verifyServices(registry, environment.getRequiredDeploymentServices(), contextId);
        if (!registry.contains(TransactionServices.class)) {
            BootstrapLogger.LOG.jtaUnavailable();
        }

        //This is the BeanManager everything happens here!!!
        BeanManagerImpl deploymentManager = BeanManagerImpl.newRootManager(finalContextId, "deployment", registry);
        setDeploymentManager(deploymentManager);

        Container.initialize(finalContextId, deploymentManager, ServiceRegistries.unmodifiableServiceRegistry(deployment.getServices()), environment);
        getContainer().setState(ContainerState.STARTING);

        tracker.start(Tracker.OP_CONTEXTS);
        final Collection<ContextHolder<? extends Context>> contexts = createContexts(registry);
        setContexts(contexts);
        tracker.end();

        final BeanDeploymentArchiveMapping bdaMapping = new BeanDeploymentArchiveMapping();
        setBdaMapping(bdaMapping);
        final DeploymentVisitor deploymentVisitor = new DeploymentVisitor(deploymentManager,
                environment, deployment, contexts, bdaMapping);
        setDeploymentVisitor(deploymentVisitor);

        if (deployment instanceof CDI11Deployment) {
            registry.add(BeanManagerLookupService.class, new BeanManagerLookupService((CDI11Deployment) deployment, bdaMapping.getBdaToBeanManagerMap()));
        } else {
            BootstrapLogger.LOG.legacyDeploymentMetadataProvided();
        }

        // Read the deployment structure, bdaMapping will be the physical structure
        // as caused by the presence of beans.xml
        tracker.start(Tracker.OP_READ_DEPLOYMENT);
        deploymentVisitor.visit();
        tracker.end();

        WeldRuntime weldRuntime = new WeldRuntime(finalContextId, deploymentManager, bdaMapping.getBdaToBeanManagerMap());
        tracker.end();
        return weldRuntime;
    }

    /**
     * Here's where we do the important ClassTransformer set up
     *
     */
    void setupInitialServices() {
        final ServiceRegistry initialServices = getInitialServices();
        final String contextId = getContextId();
        if (initialServices.contains(TypeStore.class)) {
            return;
        }
        // instantiate initial services which we need for this phase
        TypeStore store = new TypeStore();
        SharedObjectCache cache = new SharedObjectCache();
        ReflectionCache reflectionCache = ReflectionCacheFactory.newInstance(store);
        ClassTransformer classTransformer = new ClassTransformer(store, cache, reflectionCache, contextId);
        initialServices.add(TypeStore.class, store);
        initialServices.add(SharedObjectCache.class, cache);
        initialServices.add(ReflectionCache.class, reflectionCache);
        initialServices.add(ClassTransformer.class, classTransformer);
    }

    // Getters and Setters for all WeldStartup private fields

    /**
     * Gets the private deploymentManager field using reflection
     * @return the BeanManagerImpl instance
     */
    protected BeanManagerImpl getDeploymentManager() {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("deploymentManager");
            field.setAccessible(true);
            return (BeanManagerImpl) field.get(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not access 'deploymentManager' field", e);
        }
    }

    /**
     * Sets the private deploymentManager field using reflection
     * @param deploymentManager the BeanManagerImpl instance to set
     */
    protected void setDeploymentManager(BeanManagerImpl deploymentManager) {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("deploymentManager");
            field.setAccessible(true);
            field.set(this, deploymentManager);
        } catch (Exception e) {
            throw new RuntimeException("Could not set 'deploymentManager' field", e);
        }
    }

    /**
     * Gets the private bdaMapping field using reflection
     * @return the BeanDeploymentArchiveMapping instance
     */
    protected BeanDeploymentArchiveMapping getBdaMapping() {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("bdaMapping");
            field.setAccessible(true);
            return (BeanDeploymentArchiveMapping) field.get(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not access 'bdaMapping' field", e);
        }
    }

    /**
     * Sets the private bdaMapping field using reflection
     * @param bdaMapping the BeanDeploymentArchiveMapping instance to set
     */
    protected void setBdaMapping(BeanDeploymentArchiveMapping bdaMapping) {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("bdaMapping");
            field.setAccessible(true);
            field.set(this, bdaMapping);
        } catch (Exception e) {
            throw new RuntimeException("Could not set 'bdaMapping' field", e);
        }
    }

    /**
     * Gets the private contexts field using reflection
     * @return the Collection of ContextHolder instances
     */
    protected Collection<ContextHolder<? extends Context>> getContexts() {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("contexts");
            field.setAccessible(true);
            return (Collection<ContextHolder<? extends Context>>) field.get(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not access 'contexts' field", e);
        }
    }

    /**
     * Sets the private contexts field using reflection
     * @param contexts the Collection of ContextHolder instances to set
     */
    protected void setContexts(Collection<ContextHolder<? extends Context>> contexts) {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("contexts");
            field.setAccessible(true);
            field.set(this, contexts);
        } catch (Exception e) {
            throw new RuntimeException("Could not set 'contexts' field", e);
        }
    }

    /**
     * Gets the private extensions field using reflection
     * @return the List of Extension metadata
     */
    protected List<Metadata<? extends Extension>> getExtensions() {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("extensions");
            field.setAccessible(true);
            return (List<Metadata<? extends Extension>>) field.get(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not access 'extensions' field", e);
        }
    }

    /**
     * Sets the private extensions field using reflection
     * @param extensions the List of Extension metadata to set
     */
    protected void setExtensions(List<Metadata<? extends Extension>> extensions) {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("extensions");
            field.setAccessible(true);
            field.set(this, extensions);
        } catch (Exception e) {
            throw new RuntimeException("Could not set 'extensions' field", e);
        }
    }

    private void setExtensions(Iterable<Metadata<Extension>> in) {
        List<Metadata<? extends Extension>> extensions = new ArrayList<>();
        setExtensions(extensions);
        Iterables.addAll(getExtensions(), in);
    }


    /**
     * Gets the private environment field using reflection
     * @return the Environment instance
     */
    protected Environment getEnvironment() {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("environment");
            field.setAccessible(true);
            return (Environment) field.get(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not access 'environment' field", e);
        }
    }

    /**
     * Sets the private environment field using reflection
     * @param environment the Environment instance to set
     */
    protected void setEnvironment(Environment environment) {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("environment");
            field.setAccessible(true);
            field.set(this, environment);
        } catch (Exception e) {
            throw new RuntimeException("Could not set 'environment' field", e);
        }
    }

    /**
     * Gets the private deployment field using reflection
     * @return the Deployment instance
     */
    protected Deployment getDeployment() {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("deployment");
            field.setAccessible(true);
            return (Deployment) field.get(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not access 'deployment' field", e);
        }
    }

    /**
     * Sets the private deployment field using reflection
     * @param deployment the Deployment instance to set
     */
    protected void setDeployment(Deployment deployment) {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("deployment");
            field.setAccessible(true);
            field.set(this, deployment);
        } catch (Exception e) {
            throw new RuntimeException("Could not set 'deployment' field", e);
        }
    }

    /**
     * Gets the private deploymentVisitor field using reflection
     * @return the DeploymentVisitor instance
     */
    protected DeploymentVisitor getDeploymentVisitor() {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("deploymentVisitor");
            field.setAccessible(true);
            return (DeploymentVisitor) field.get(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not access 'deploymentVisitor' field", e);
        }
    }

    /**
     * Sets the private deploymentVisitor field using reflection
     * @param deploymentVisitor the DeploymentVisitor instance to set
     */
    protected void setDeploymentVisitor(DeploymentVisitor deploymentVisitor) {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("deploymentVisitor");
            field.setAccessible(true);
            field.set(this, deploymentVisitor);
        } catch (Exception e) {
            throw new RuntimeException("Could not set 'deploymentVisitor' field", e);
        }
    }

    /**
     * Gets the private initialServices field using reflection
     * @return the ServiceRegistry instance
     */
    protected ServiceRegistry getInitialServices() {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("initialServices");
            field.setAccessible(true);
            return (ServiceRegistry) field.get(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not access 'initialServices' field", e);
        }
    }

// Note: No setter for initialServices because it's final

    /**
     * Gets the private contextId field using reflection
     * @return the contextId String
     */
    protected String getContextId() {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("contextId");
            field.setAccessible(true);
            return (String) field.get(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not access 'contextId' field", e);
        }
    }

    /**
     * Sets the private contextId field using reflection
     * @param contextId the contextId String to set
     */
    protected void setContextId(String contextId) {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("contextId");
            field.setAccessible(true);
            field.set(this, contextId);
        } catch (Exception e) {
            throw new RuntimeException("Could not set 'contextId' field", e);
        }
    }

    /**
     * Gets the private tracker field using reflection
     * @return the Tracker instance
     */
    protected Tracker getTracker() {
        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("tracker");
            field.setAccessible(true);
            return (Tracker) field.get(this);
        } catch (Exception e) {
            throw new RuntimeException("Could not access 'tracker' field", e);
        }
    }

    /**
     * Calls the private checkApiVersion() method from the parent class using reflection
     * Protected method to allow access from inheritors
     */
    void checkApiVersion() {
        try {
            Method checkApiVersionMethod = this.getClass().getSuperclass().getDeclaredMethod("checkApiVersion");
            checkApiVersionMethod.setAccessible(true);
            checkApiVersionMethod.invoke(this);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find 'checkApiVersion' method in parent class", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access 'checkApiVersion' method in parent class", e);
        } catch (InvocationTargetException e) {
            // Unwrap the actual exception thrown by the method
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException("Unexpected exception from checkApiVersion", cause);
            }
        }
    }

    /**
     * Calls the private getContainer() method from the parent class using reflection
     * Protected method to allow access from inheritors
     *
     * @return the Container instance
     */
    protected Container getContainer() {
        try {
            Method getContainerMethod = this.getClass().getSuperclass().getDeclaredMethod("getContainer");
            getContainerMethod.setAccessible(true);
            return (Container) getContainerMethod.invoke(this);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find 'getContainer' method in parent class", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access 'getContainer' method in parent class", e);
        } catch (InvocationTargetException e) {
            // Unwrap the actual exception thrown by the method
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException("Unexpected exception from getContainer", cause);
            }
        }
    }

    /**
     * Calls the private addImplementationServices(ServiceRegistry) method from the parent class using reflection
     * Protected method to allow access from inheritors
     *
     * @param services the ServiceRegistry to configure with implementation services
     */
    protected void addImplementationServices(ServiceRegistry services) {
        try {
            Method addImplementationServicesMethod = this.getClass().getSuperclass()
                    .getDeclaredMethod("addImplementationServices", ServiceRegistry.class);
            addImplementationServicesMethod.setAccessible(true);
            addImplementationServicesMethod.invoke(this, services);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find 'addImplementationServices' method in parent class", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access 'addImplementationServices' method in parent class", e);
        } catch (InvocationTargetException e) {
            // Unwrap the actual exception thrown by the method
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException("Unexpected exception from addImplementationServices", cause);
            }
        }
    }
}
