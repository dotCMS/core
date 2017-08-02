package com.dotcms.api.di.module;

/**
 * An implementation of a Module Configurator will provide a set of Class or class names. In addition you can
 * also provide a packages or packages names to be scanned.
 * If you use package scanning keep in mind you might have to annotated your classes with {@link com.dotcms.api.di.DotBean},
 * it will tell to dotCMS that you want to include the class to the injector.
 * If the classes implements any interface, the first interface will be used as a key, otherwise the class itself will use as a
 * key.
 * @author jsanca
 */
public interface ModuleConfigurator {

    default Class<?>[] classes () {
        return null;
    }
    default String  [] classesNames () {
        return null;
    }
    default Package [] packages() {
        return null;
    }
    default String  [] packagesNames () {
        return null;
    }

} // E:O:F:ModuleConfigurator.
