
README
------

This bundle plugin is an example of how to add a simple fragment bundle that can be use it to export 3rd party libraries

How to create a bundle fragment plugin
-------------------------------------------

--
In order to add a fragment OSGI bundle plugin, you must write the META-INF/MANIFEST
to be inserted into the OSGI jar.

In this MANIFEST you must specify (see template plugin):

Bundle-Name: The name of your fragment

Bundle-SymbolicName: A short an unique name for the fragment

Fragment-Host: system.bundle; extension:=framework

Export-Package: This is a comma separated list of package's name.
                In this list there must be the packages that you want to make
                available for other bundles.

--
--
--
Fragment bundles
---------

A Bundle fragment, is a bundle whose contents are made available to another bundles exporting 3rd party libraries.
One notable difference is that fragments do not participate in the lifecycle of the bundle, and therefore cannot have an Bundle-Activator.
As it not contain a Bundle-Activator a fragment cannot be started so after deploy it will have its state as Resolved and NOT as Active as a normal bundle plugin.
