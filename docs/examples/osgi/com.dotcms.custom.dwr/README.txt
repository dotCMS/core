
README
------

This bundle plugin is an example of how to add a custom DWR servlet to a bundle plugin.

How to create a bundle plugin with DWR
-------------------------------------------

--
In order to create this OSGI plugin, you must write the META-INF/MANIFEST
to be inserted into OSGI jar.

In this MANIFEST you must specify (see template plugin):

Bundle-Name: The name of your bundle

Bundle-SymbolicName: A short an unique name for the bundle

Bundle-Activator: Package and name of your Activator class (example: com.dotmarketing.osgi.custom.dwr.Activator)

Bundle-ClassPath: The Bundle-ClassPath specifies where to load classes from from the bundle.
                  This is a comma separated list of elements to load (such as current folder,lib.jar,other.jar).
                  (example: .,lib/dwr_3rc2modified.jar )

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

com.dotmarketing.osgi.custom.dwr.ajax
-------------------------------------

com.dotmarketing.osgi.custom.dwr.ajax.CustomDWRAjax 
----------
This class will provide a simple implementation class to use with DWR.
--

com.dotmarketing.osgi.custom.dwr.osgi
-------------------------------------

com.dotmarketing.osgi.custom.dwr.osgi.Activator
---------
This bundle activator extends from com.dotmarketing.osgi.GenericBundleActivator and implements BundleActivator.start().
Creates a stand-alone DWR servlet in the context of the plugin.
--

WEB-INF
-------
Folder containing DWR configuration files. DWR will not run without these files present.
--

META-INF
-------
Folder containing MANIFEST.MF
--

lib
-------
Folder containing dwr_3rc2modified.jar
--

info
-------
Folder containing required Exported Packages. (exported_packages_list.txt)
The DotCMS must declare the set of packages that will be available to
the OSGI plugins by changing the file: dotCMS/WEB-INF/felix/osgi-extra.conf.
This is possible also using the dotCMS UI (CMS Admin->Dynamic Plugins->Exported Packages).
--
