package com.dotcms.jersey;

import javax.enterprise.inject.spi.BeanManager;
import org.glassfish.jersey.internal.inject.InjectionManager;

public class DotGenericCdiBeanSupplier extends DotAbstractCdiBeanSupplier  {

    public DotGenericCdiBeanSupplier(Class rawType,
            InjectionManager injectionManager,
            BeanManager beanManager,
            boolean cdiManaged) {
        super(rawType, injectionManager, beanManager, cdiManaged);
    }


    @Override
    public Object get() {
        return _provide();
    }
}
