
README
------

This bundle plugin is an example of how to add dotcms hooks classes with our bundle plugin.

How to build this example
-------------------------

To install all you need to do is build the JAR. to do this run
./gradlew jar
This will build a jar in the build/libs directory

1. To install this bundle:

Copy the bundle jar file inside the Felix OSGI container (dotCMS/felix/load).
        OR
Upload the bundle jar file using the dotCMS UI (CMS Admin->Dynamic Plugins->Upload Plugin).

2. To uninstall this bundle:

Remove the bundle jar file from the Felix OSGI container (dotCMS/felix/load).
        OR
Undeploy the bundle using the dotCMS UI (CMS Admin->Dynamic Plugins->Undeploy).

How to create a bundle plugin for dotcms hook classes
-------------------------------------------

--
In order to create this OSGI plugin, you must create a META-INF/MANIFEST to be inserted into OSGI jar.
This file is being created for you by Gradle. If you need you can alter our config for this but in general our out of the box config should work.
The Gradle plugin uses BND to generate the Manifest. The main reason you need to alter the config is when you need to exclude a package you are including on your Bundle-ClassPath

If you are building the MANIFEST on your own or desire more info on it below is a description of what is required
in this MANIFEST you must specify (see template plugin):

Bundle-Name: The name of your bundle

Bundle-SymbolicName: A short an unique name for the bundle

Bundle-Activator: Package and name of your Activator class (example: com.dotmarketing.osgi.hooks.Activator)

DynamicImport-Package: *
    Dynamically add required imports the plugin may need without add them explicitly

Import-Package: This is a comma separated list of package's name.
                In this list there must be the packages that you are using inside
                the bundle plugin and that are exported by the dotCMS runtime.

Beware!!!
---------

In order to work inside the Apache Felix OSGI runtime, the import
and export directive must be bidirectional.

The DotCMS must declare the set of packages that will be available to
the OSGI plugins by changing the file: dotCMS/WEB-INF/felix/osgi-extra.conf.
This is possible also using the dotCMS UI (CMS Admin->Dynamic Plugins->Exported Packages).

Only after that exported packages are defined in this list,
a plugin can Import the packages to use them inside the OSGI blundle.

--
--
--
com.dotmarketing.osgi.hooks.SamplePostContentHook AND com.dotmarketing.osgi.hooks.SamplePreContentHook
-----------------------------------------------

Hooks classes that will override the contentletCount method to see how they work.

--
Activator
---------

This bundle activator extends from com.dotmarketing.osgi.GenericBundleActivator and implements BundleActivator.start().
Calls ContentletAPI.contentletCount() who will fire ours hooks methods.
