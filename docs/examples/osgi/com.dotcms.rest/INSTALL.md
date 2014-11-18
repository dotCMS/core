
# README
----
This is an example of how to create and load Jersey Based REST resources in dotCMS via OSGi 


## How to build this example
----

To install all you need to do is build the JAR. To do this run from this directory:

`./gradlew jar`

or for windows

`.\gradlew.bat jar`

This will build a jar in the build/libs directory

### To install this bundle

Copy the bundle jar file inside the Felix OSGI container (dotCMS/felix/load).
        OR
Upload the bundle jar file using the dotCMS UI (CMS Admin->Dynamic Plugins->Upload Plugin).

### To uninstall this bundle:

Remove the bundle jar file from the Felix OSGI container (dotCMS/felix/load).
        OR
Undeploy the bundle using the dotCMS UI (CMS Admin->Dynamic Plugins->Undeploy).



## How to test
----

Once installed, you can access this resource by (this assumes you are on localhost)

`http://localhost:8080/api/example`

or this, which requires an dotcms user to access(See authentication below)

`http://localhost:8080/api/example/auth`


You can try the put and post resources by

`curl -XPUT http://localhost:8080/api/example`

`curl -XPOST http://localhost:8080/api/example`




## Authentication
----
This API supports the same REST auth infrastructure as other 
rest apis in dotcms. There are 4 ways to authenticate.

* user/xxx/password/yyy in the URI
* basic http/https authentication (base64 encoded)
* DOTAUTH header similar to basic auth and base64 encoded, e.g. setHeader("DOTAUTH", base64.encode("admin@dotcms.com:admin"))
* Session based (form based login) for frontend or backend logged in user