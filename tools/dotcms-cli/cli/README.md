# dotCMS CLI 

Welcome to the cli readme   
Here I'll explain how the tool can be used

The CLI is a quarkus/pico-cli project that depends on module api-data-module for which that dependency should have been built already see top level read-me for more details on how to build this entire project.  
Assuming the that api-data-module has been built already    

```shell script
# from top level to build all
../mvnw clean install
```

To run cli. 

First Start a dotCMS instance locally. The CLI is a client program designed to simplify access to dotCMS content services. So we're going to need a running instance of dotCMS to point our client to  

## Running The CLI in dev mode

```shell script
# from top level to build all
cd cli
# command is same as the following to run the quarkus build plugin
# ../mvnw quarkus:dev 
quarkus dev
```
By executing the command described above we launch the cli within Quarkus dev environment 
Meaning we can perform changes to our sources and see them reflected live. So be mindful about it.
This is the only quarkus module within our project so any attempt to run mvnw quarkus:dev 
in any other module is going to result in failure and that is expected

Once the cli is launched in dev mode it'll print out a list of available commands.

followed by 

```shell script
--
Tests paused
Press [space] to restart, [e] to edit command line args (currently ''), [r] to resume testing, [o] Toggle test output, [:] for the terminal, [h] for more options>
```
These are quarkus offered options. 
Now in order to see the CLI in action you need to edit the command line arguments and introduce the cli command and arguments of choice. for example.

```shell script
--
Tests paused
status
```
Which provides you with a quick summary of your status as user. 

or

```shell script
--
Tests paused
login --user=admin@dotCMS.com --password=admin
```
Which allows you to login against an environment. 

Now you're probably asking your self how do I instruct the cli what instance of dotCMS I'm running against.

```shell script
--
Tests paused
instance --list
```
Which allows you to list and select the dotCMS instance we want to run against

in order to see all available command options simply introduce the command name followed by --help
a list of all available commands will appear upon executing the cli with no arguments 

We can also instruct Quarkus dev mode to launch our cli using a preconfigured param by doing 

```shell script
mvn quarkus:dev -Dquarkus.args=status
```
This will launch the cli passing directly into it the arguments that tell them to execute the command status

## Running The CLI as a jar

In order to generate the cli as a jar packed with all necessary dependencies you need to do

```shell script
./mvnw clean install package
```
all the commands used above can be applied directly to the generated jar which can be found under 

```shell script
./cli/target/quarkus-app/
```
Like this 

```shell script
java -jar ./cli/target/quarkus-app/quarkus-run.jar status
```

# Available Commands
For a complete list of available commands you can call the CLI with no params.
This will provide a list of available commands, a quick guide of functionality and the expected params.

From now on the commands will be presented as if they were invoked directly from the command line
Without executing or running the quarkus dev environment first

## **Instance Command**

Instance allows the activation of a dotCMS instance profile
to get a list with all available profiles do:
```shell script
   dotCMS instance  
```

```
Available registered dotCMS servers.
 Profile [default], Uri [http://localhost:8080/api], active [yes].
 Profile [demo], Uri [https://demo.dotcms.com/api], active [no].
```

From the list of available instances select one and then activate it using the activate option like this
```shell script
instance --activate demo
```

Once you have activated the new instance you want to see what your current status again is.

## **Status Command**

Status is how you determine what dotCMS instance you are connected to
Also tells you if your currently logged in that selected instance

```shell script
   dotCMS status  
```

```
2023-02-22 11:25:29,499 INFO  [com.dot.api.cli.HybridServiceManagerImpl] (Quarkus Main Thread) Service [default] is missing credentials.
2023-02-22 11:25:29,500 INFO  [com.dot.api.cli.HybridServiceManagerImpl] (Quarkus Main Thread) Service [demo] is missing credentials.
Active instance is [demo] API is [https://demo.dotcms.com/api] No active user Use login Command.
```
Please note that the output of the status command says **demo** is the selected  instance

These instances or dotCMS profiles are loaded from file located under user home user a hidden folder named .dotCMS
The file is dot-service.yml and the contents looks more or less like this:

```
---
- name: "default"
- name: "demo"
  active: true
```

### **Important:** This file is where new instances should be registered.

## **Login Command**
 Once you have selected a dotCMS instance you have to log in to be able to interact with it. 
 and that is accomplished through the login command as follows

```shell script
   dotCMS login --user=admin@dotCMS.com --password=admin  
```

## **Content-Type Command**

Content-Type command is how you do CRUD operations on CTs

**Find Content-type**
```shell script
   dotCMS content-types find 
```
Gets you a complete list of the available Content-types. By Default, they are shown in batches of 10.
But you can also get an entire list at one by specifying  --interactive=false 

Find Sites
```shell script
   dotCMS content-types find --interactive=false 
```

For a name like type of search do as follows 
```shell script
   dotCMS content-types --name="File" --page=0 --pageSize=10
```
This should bring back all content-types matching the name "File"

**Pull Content-type**
```shell script
   dotCMS content-types pull FileAsset 
```
This will get you a descriptor of the solicited content-type in this case "FileAsset" 
Saving it as a file using the content-type variable name as file name. 
  
All content info is presented by default in json format.
However. this is a preference that can be modified by specifying the attribute format
Like this 

**Pull Content-type**
```shell script
   dotCMS content-types pull "FileAsset" --format=YML 
```

**Push Content-type**
The next command is Push which allows you to create a new content-type by sending the CT definition using a file as follows

```shell script
   dotCMS content-types push "./content-type.json" 
```

Again here we can change the format of the input file by using the option --format like this:

```shell script
   dotCMS content-types push "./content-type.yml" --format=YML 
```

## **Site Command**
Site command is how you do CRUD operations on sites

```shell script
   dotCMS site find
```
Gets you a list of all the published sites

```
name: [default] id: [8a7d5e23-da1e-420a-b4f0-471e7da8ea2d] inode: [1b407535-67e3-4f21-ad00-8ef5ef492d64] live:[yes] default: [yes] archived: [no]
name: [demo2.dotcms.com] id: [28c9ce6b2147e0f4763ee2ee5628faeb] inode: [b81879e0-5f81-4cf9-9cc3-86a048f1203c] live:[no] default: [no] archived: [no]
```

From the list of sites pick the one that interests you and to get more details do
```shell script
   dotCMS site pull demo2.dotcms.com
```
This command gets you a representation of the site info in json format. Saving it immediately as file using as name the site-name itself.

```shell script
   dotCMS site push "./site-descriptor.json" 
```
or 

```shell script
   dotCMS site push "./site-descriptor.yml"  --format=YML 
```

There's another useful command that can be used to quickly kick off a site  by simply providing a name 

```shell script
   dotCMS site create "my.cool.bikes.site.com" 
```
Once a site has been created you need to start it or stop it. and that can be accomplished with the two following examples respectively

```shell script
   dotCMS site start "my.cool.bikes.site.com" 
```
And 

```shell script
   dotCMS site stop "my.cool.bikes.site.com" 
```

And finally here's how you remove sites

First you need to archive the site 

```shell script
   dotCMS site archive "my.cool.bikes.site.com" 
```

And then

```shell script
   dotCMS site remove "my.cool.bikes.site.com" 
```

