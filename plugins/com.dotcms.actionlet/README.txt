
README
------

How to add a ViewTool OSGI plugin
---------------------------------

In order to add a ViewTool OSGI plugin, you must write the META-INF/MANIFEST
to be inserted into OSGI jar.

In this MANIFEST you must specify (see template plugin):

Bundle-Name: The name of your view tool

Bundle-Activator: The main activator (it must be a subclass of 
com.dotmarketing.osgi.AbstractViewToolActivator to automatize 
the whole process)

Bundle-SymbolicName: A short name for the ViewTool

Import-Package: This is a comma separated list of package's name.
In this list there must be the packages that you are using inside
the ViewTool plugin and that are exported by the dotCMS runtime.

In this example you will find the package:
- org.apache.velocity.tools.view for interface ToolInfo
- org.apache.velocity.tools.view.tools for interface ViewTool
- com.dotmarketing.osgi for the AbstractViewToolActivator

Beware!!!
---------

In order to work inside the Apache Felix OSGI runtime, the import
and export directive must be bidirectional.

The DotCMS must declare the set of packages that will be available
to the OSGI plugins by changing the property:
org.osgi.framework.system.packages.extra
inside the Felix configuration file dotCMS/felix/config/config.properties

Only after that exported packages are defined in this list, 
a plugin can Import the pacakges to use them inside the OSGI ViewTool.

Notice
------

It is suggested to export/import only interfaces that are shared between
dotCMS core and the ViewTool plugin


