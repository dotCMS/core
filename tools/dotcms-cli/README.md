# Quarkus CLI Data Model example

This project acts as a maven reactor pom build 
You can build at the top level to build all projects

To run individual services or quarkus dev mode cd to the module you want to run
making sure that dependencies have been installed first 

```shell script
# from top level to build all
./mvnw install
```

To run example API in dev mode
```shell script
# from top level to build all
cd rest-api
# command is same as the following to run the quarkus build plugin
# ../mvnw quarkus:dev 
quarkus dev
```

To run cli

To run example API in dev mode
```shell script
# from top level to build all
cd rest-api
# command is same as the following to run the quarkus build plugin
# ../mvnw quarkus:dev 
quarkus dev
```
NOTE:  To reduce duplication in the multi module project mvnw is not included in each submodule. 
The quarkus command finds the executable


To run mvnw from a submodule just use a relative path to the parent mvn
if running from the submodule folder all dependencies will need to be up to date and instlled to the local mvn with maven install

Alternatively you can specify the sub project from the parent folder
```shell script
# from top level to build all
cd cli
# command is same as the following to run the quarkus build plugin
# ../mvnw quarkus:dev 
./mvnw -pl cli quarkus:dev
```

quarkus:dev can only run a single project, but for other maven options you can choose to work out the dependencies to build and skip unrelated submodules
this uses the --am (also make) option.  In this way with the cli module will build the api-data-model but not build the rest-api module

```shell script
# from top level to build all
# command is same as the following to run the quarkus build plugin
# ../mvnw quarkus:dev 
./mvnw -pl cli --am install
```


## Project outline
### api-data-model
Should have minimal dependencies, this uses Immutables to generate model objects.   Immutables generates java code
that gets built as regular classes into jar so Immutables does not need to be a dependency on any code that uses this
This should declare any shared interfaces and data objects that are parameters and return objects of those interfaces.
This does not need to be built with quarkus

org.acme.model contains a copy of most of existing Contentlet model we have as and example

org.acme.newmodel contains some objects created by looking at existing API classes and json to
create an Immutable object that is compatible at a JSON level.   It also shows an example of using
request and response immutable classes as well as using generics in the interface to share common functionality


### rest-api
This module implements the base API defined in api-data-model using JAX-RS to
provide a Rest interface for calling the api.   We make the rest classes call a delegate implementation
of the service interface,  we could make the resource class implement the service interface itself
but not doing so can provide some flexibility, e.g. you may not want all the public service methods to be rest calls
or you may need to do some special JAX-RS handling to implement the method.  
Any REST specific handling, e.g. authentication, Exception Mapping, Response object,  Streaming handling
can be done in here that are not concerns of the underlying java interface.

If the project using this allows for CDI it would be easy to add the API and inject the service implementations
Otherwise the owner will need to manage the annotation processing itself and methods will need to be created to allow manual injection of the implementation

### cli

This module uses picocli to easily create a client application

https://quarkus.io/guides/picocli
https://picocli.info/

The creation of individual subcommands becomes easy with picocli,  the current app demonstrates
some examples of what we can do.

####ContentletCommand
This shows an example of use calling our service apis directly without rest.
####CContentletRemoteCommand
This shows an example of calling a rest service we created in rest-api.   
We are using Microprofile Rest Client to make connection to the api easy and reflective of
the annotations that are used on the server.  
####CLoginCommand
Shows example of getting a token from demo.cms.com and storing it in users personal secure storage
####StatusCommand
Checks the token status against the server and returns current user object.
####org.acme.restclient
Contains the Micropofile Rest Client classes.


## Docker
Checkout this link with a good description of the containerization and docker build options

http://www.mastertheboss.com/soa-cloud/quarkus/building-container-ready-native-applications-with-quarkus/


## Configuration
Microprofile Configuration with quarkus makes configuration amazingly simple.   First place for all configuration
in the code is to look in the resources/application.properties file.   

https://quarkus.io/guides/config-reference


https://quarkus.io/get-started/



---



This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/code-with-quarkus-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Provided Code

### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
