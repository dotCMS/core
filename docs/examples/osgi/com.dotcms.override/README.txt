
README
------

This bundle plugin is an example of how to override dotcms classes with our bundle plugin.

How to create a bundle plugin to override dotcms classes
-------------------------------------------

--
In order to create this OSGI plugin, you must write the META-INF/MANIFEST
to be inserted into OSGI jar.

In this MANIFEST you must specify (see template plugin):

Bundle-Name: The name of your bundle

Bundle-SymbolicName: A short an unique name for the bundle

Bundle-Activator: Package and name of your Activator class (example: com.dotmarketing.osgi.override.Activator)

Override-Classes: This is a comma separated list of classes.
                  In this list there must be the classes we are trying to
                  override, in order to override any dotcms class is mandatory
                  to add it to this list because dotcms implementation of the class will be
                  already loaded by dotcms classloader so if we don't explicit
                  specify the classes to override the classloader wont try to
                  load them.

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
com.dotmarketing.util.FileUtil
-----------------------------------------------

The com.dotmarketing.util.FileUtil is a utility class in dotcms, for this example
we are creating our custom com.dotmarketing.util.FileUtil that will override the
dotcms implementation with ours.
Our com.dotmarketing.util.FileUtil class will have just one test method that will
print a message in console if everything is working properly.

--
Activator
---------

This bundle activator extends from com.dotmarketing.osgi.GenericBundleActivator and implements BundleActivator.start().
Calls our implementation of the com.dotmarketing.util.FileUtil class.

* PLEASE note the "publishBundleServices( context )" call, this call is MANDATORY (!) as it will allow us to share resources
  between the bundle and the host container (dotcms).
