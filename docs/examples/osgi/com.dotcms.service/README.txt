
README
------

This bundle plugin is an example of how to add a simple service class that can be use it by others bundles inside the Felix OSGI container (dotCMS/felix/load)

How to create a bundle plugin with services
-------------------------------------------

--
In order to add a service OSGI plugin, you must write the META-INF/MANIFEST
to be inserted into OSGI jar.

In this MANIFEST you must specify (see template plugin):

Bundle-Name: The name of your bundle

Bundle-SymbolicName: A short an unique name for the bundle

Bundle-Activator: Package and name of your Activator class (example: com.dotmarketing.osgi.service.Activator)

Export-Package: This is a comma separated list of package's name.
                In this list there must be the packages that you want to make
                available for other bundles.

Import-Package: This is a comma separated list of package's name.
                In this list there must be the packages that you are using inside
                the bundle plugin and that are exported by the dotCMS runtime.

Beware!!!
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
Exposed service
---------

The exposed service for this example will be a simple Interface class with it implementation (com.dotmarketing.osgi.service.HelloWorld AND com.dotmarketing.osgi.service.HelloWorldService).

--
Activator
---------

This bundle activator extends from com.dotmarketing.osgi.GenericBundleActivator and implements BundleActivator.start().
Registers an instance of a our test service using the bundle context; and attaches properties to the service that can be queried
when performing a service look-up.
