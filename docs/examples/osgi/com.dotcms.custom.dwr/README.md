#README

This bundle plugin is an example of how to add a custom DWR servlet to a bundle plugin.

### How to build this example

To install all you need to do is build the JAR. to do this run 
```
./gradlew jar
```
This will build a jar in the build/libs directory

### To install this bundle:

Upload the bundle jar file using the dotCMS UI (*CMS Admin->Dynamic Plugins->Upload Plugin*).
	
### To uninstall this bundle:

Undeploy the bundle using the dotCMS UI (*CMS Admin->Dynamic Plugins->Undeploy*).


###How to create a bundle plugin with DWR

In order to create an OSGI plugin, you must create a *META-INF/MANIFEST* to be included in the OSGI jar.
This file is being created for you by Gradle. If you need you can alter our config for this but in general our out of the box config should work.
The Gradle plugin uses BND to generate the Manifest. The main reason you need to alter the config is when you need to exclude a package you are including on your Bundle-ClassPath

In this *MANIFEST* you must specify (see the included plugin as an example):

* *Bundle-Name*: The name of your bundle
* *Bundle-SymbolicName*: A short an unique name for the bundle
* *Bundle-Activator*: Package and name of your Activator class (example: *com.dotmarketing.osgi.custom.spring.Activator*)
* *Import-Package*: This is a comma separated list of the names of packages to import. In this list there must be the packages that you are using inside your osgi bundle plugin and are exported and exposed by the dotCMS runtime.


### Beware (!)

In order to work inside the Apache Felix OSGI runtime, the import and export directive must be bidirectional.

As of dotcms 2.5.2 if you do not start with a **dotCMS/WEB-INF/felix/osgi-extra.conf** ALL packages will be exported for you. So there is nothing for you to do

The DotCMS must declare the set of packages that will be available to the OSGI plugins by changing the file: *dotCMS/WEB-INF/felix/osgi-extra.conf*.
This is possible also using the dotCMS UI (*CMS Admin->Dynamic Plugins->Exported Packages*).

Only after that exported packages are defined in this list, a plugin can Import the packages to use them inside the OSGI blundle.

## Components

### com.dotmarketing.osgi.custom.dwr.ajax.CustomDWRAjax 

This class will provide a simple implementation class to use with DWR.

### com.dotmarketing.osgi.custom.dwr.osgi.Activator

This bundle activator extends from *com.dotmarketing.osgi.GenericBundleActivator* and implements `BundleActivator.start()`.
Creates a stand-alone DWR servlet in the context of the plugin.

### WEB-INF

Folder containing DWR configuration files. DWR will not run without these files present.

* PLEASE note the `publishBundleServices( context )` call, this call is MANDATORY (!) as it will allow us to share resources between the bundle and the host container (dotCMS).

________________________________________________________________________________________

## Testing

```

     <script type='text/javascript' src='/app/custom_dwr/engine.js'></script>
     <script type='text/javascript' src='/app/custom_dwr/util.js'></script>
     <script type='text/javascript' src='/app/custom_dwr/interface/CustomDWRAjax.js'></script>

     <script type='text/javascript'>

         function sayHelloDWR() {

             var name = 'dotCMS User!';
             CustomDWRAjax.getHello( name, sayHelloCallback );
         }

         function sayHelloCallback( data ) {

             if ( data[ "message" ] != null ) {
                 var messageData = data[ "message" ];
                 alert( "DWR says: " + messageData );
             }
         }

         sayHelloDWR();

     </script>
         
```