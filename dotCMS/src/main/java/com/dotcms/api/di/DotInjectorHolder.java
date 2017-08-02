package com.dotcms.api.di;


import com.dotcms.api.di.module.CoreModule;
import com.google.inject.Guice;

public class DotInjectorHolder {

    private static DotInjector dotInjector =
            new DotInjectImpl(Guice.createInjector(new CoreModule()));

    public static DotInjector getInjector () {

        return dotInjector;
    }
}
