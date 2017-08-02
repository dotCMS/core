package com.dotcms.rest.config;


import com.dotcms.api.di.DotInjectorHolder;
import com.dotcms.repackage.javax.inject.Singleton;
import com.dotcms.repackage.org.glassfish.hk2.api.DynamicConfigurationService;
import com.dotcms.repackage.org.glassfish.hk2.api.ServiceLocator;
import com.dotcms.repackage.org.glassfish.hk2.utilities.BuilderHelper;
import com.dotcms.repackage.org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;
import com.dotcms.repackage.org.jvnet.hk2.internal.*;

public class DotServiceLocatorGenerator extends ServiceLocatorGeneratorImpl {

    @Override
    public ServiceLocator create(final String name, final ServiceLocator parent) {
        final ServiceLocator serviceLocator = this.initialize(name, parent);

        /*final DotInjector injector = DotInjectorHolder.getInjector()
                .addModule(new DotGuiceModuleIntegrator(serviceLocator));

        ServiceLocatorUtilities.addOneConstant(serviceLocator,
                new DotInjectionResolver(injector, serviceLocator));*/

        //return new DotInjectorServiceLocator(serviceLocator, DotInjectorHolder.getInjector());
        return serviceLocator;
    }

    private ServiceLocatorImpl initialize(String name, ServiceLocator parent) {
        if(parent != null && !(parent instanceof ServiceLocatorImpl)) {
            throw new AssertionError("parent must be a " + ServiceLocatorImpl.class.getName() + " instead it is a " + parent.getClass().getName());
        } else {
            ServiceLocatorImpl sli = new DotServiceLocatorImpl(name, (ServiceLocatorImpl)parent, DotInjectorHolder.getInjector());
            DynamicConfigurationImpl dci = new DynamicConfigurationImpl(sli);
            dci.bind(Utilities.getLocatorDescriptor(sli));
            dci.addActiveDescriptor(Utilities.getThreeThirtyDescriptor(sli));
            dci.bind(BuilderHelper.link(DynamicConfigurationServiceImpl.class, false).to(DynamicConfigurationService.class).in(Singleton.class.getName()).localOnly().build());
            dci.bind(BuilderHelper.createConstantDescriptor(new DefaultClassAnalyzer(sli)));
            dci.bind(BuilderHelper.createDescriptorFromClass(ServiceLocatorRuntimeImpl.class));
            dci.commit();
            return sli;
        }
    }

}
