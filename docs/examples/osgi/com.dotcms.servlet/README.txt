
README
------

This bundle plugin is an example of how to use services provide by other bundles and
 how to register servlets and filters.

How to create a bundle plugin using services and registering servlets and filters
-------------------------------------------

--
In order to create this OSGI plugin, you must write the META-INF/MANIFEST
to be inserted into OSGI jar.

In this MANIFEST you must specify (see template plugin):

Bundle-Name: The name of your bundle

Bundle-SymbolicName: A short an unique name for the bundle

Bundle-Activator: Package and name of your Activator class (example: com.dotmarketing.osgi.servlet.Activator)

DynamicImport-Package: *
    Dynamically add required imports the plugin may need without add them explicitly

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
com.dotmarketing.osgi.servlet.HelloWorldServlet
-----------------------------------------------

Simple and standard implementation of a HttpServlet that will use
the HelloWorld service provide by the com.dotcms.service bundle plugin.

--
com.dotmarketing.osgi.servlet.TestFilter
----------------------------------------

Simple and standard implementation of a Filter

--
Activator
---------

This bundle activator extends from com.dotmarketing.osgi.GenericBundleActivator and implements BundleActivator.start().
Gets a reference for the HelloWorldService via HelloWorld interface (com.dotcms.service bundle plugin) and register
our HelloWorldServlet servlet and the TestFilter filter.

--
--
--
Testing
-------

The HelloWorldServlet is registered under the url pattern "/helloworld" can be test it running and assuming you dotcms url is localhost:80880:
    http://localhost:8080/dynamic/helloworld

The TestFilter filter is registered for the url pattern "/helloworld/.*" can be test it running and assuming you dotcms url is localhost:80880:
    http://localhost:8080/dynamic/helloworld/
    http://localhost:8080/dynamic/helloworld/testing.dot
