
README
------

This bundle plugin is an example of how to Schedule Quartz Jobs using an OSGI bundle plugin.

How to create a bundle plugin for Schedule Quartz Jobs
-------------------------------------------

--
In order to create this OSGI plugin, you must write the META-INF/MANIFEST
to be inserted into OSGI jar.

In this MANIFEST you must specify (see template plugin):

Bundle-Name: The name of your bundle

Bundle-SymbolicName: A short an unique name for the bundle

Bundle-Activator: Package and name of your Activator class (example: com.dotmarketing.osgi.job.Activator)

DynamicImport-Package: *
    Dynamically add required imports the plugin may need without add them explicitly

Import-Package: This is a comma separated list of package's name.
                In this list there must be the packages that you are using inside
                the bundle plugin and that are exported by the dotCMS runtime.

#Beware!!!
---------

In order to work inside the Apache Felix OSGI runtime, the import
and export directive must be bidirectional.

The DotCMS must declare the set of packages that will be available
to the OSGI plugins by changing the property:
felix.org.osgi.framework.system.packages.extra
inside the configuration file src-conf/dotmarketing-config.properties

Only after that exported packages are defined in this list,
a plugin can Import the packages to use them inside the OSGI blundle.

--
--
--
com.dotmarketing.osgi.job.CustomJob
-----------------------------------------------

Simple Job class that implements the regular Quartz Job interface

--
Activator
---------

This bundle activator extends from com.dotmarketing.osgi.GenericBundleActivator and implements BundleActivator.start().
Will manually register a *CronScheduledTask* making use of the method *scheduleQuartzJob*

* PLEASE note the "unregisterServices()" call, this call is MANDATORY (!) as it will allow us to stop and remove the register Quartz Job.

--
--
--
Limitations (!)
-------

There are limitations on the hot deploy functionality for the OSGI Quartz Jobs plugins, once you upload this plugin you will have some limitations
on what code you can modify for the Quartz Job.

The java hot swapping allows to redefine (Reload) classes, but this are the limitations:

    The redefinition may change method bodies, the constant pool and attributes.
    The redefinition must not add, remove or rename fields or methods, change the signatures of methods, or change inheritance.

This limitations will apply only for the OSGI Quartz plugins because in order to integrate our OSGI plugins with the dotCMS/Quartz code we had to
work outside the OSGI and the plugin context.
