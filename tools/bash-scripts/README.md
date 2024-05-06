
# DEPRECATION
These scripts are deprecated and no longer maintained. Please refer to the [dotCMS CLI](../dotcms-cli/README.md)

# dot-cli

This is a Bash Command Line Interface (CLI) to interact with dotCMS to simplify the interaction with Our Rest endpoints.  

It allows you to create update ContentTypes, Sites, Folder, and File Assets
 

https://github.com/dotCMS/core

## Pre Requisites

This CLI relies on 
[jq](https://stedolan.github.io/jq)
Find a suitable version or the instructions to get it installed via package manager here:
[here](https://stedolan.github.io/jq/download/)

## Usage

The tool expects a directory like structure holding all the json descriptor/files to be pushed   
The expected directory structure looks like this:

<pre>

 /bundle
  |- /sites
      |- SYSTEM_HOST.site.json
      |- demo.dotcms.com.site.json
      / content-types
        |- blog.contentype.json
        |- story.contentype.json
      / files
          |- en-US 
          |   |- demo.dotcms.com (site)
          |   |  |- images (Folder)
          |   |  |  |- file1
          |   |  |  |- file2
          |   |  |- application (Folder)
          |   |     |- file3
          |   |     |- file4
          |   |
          |   |- authoring.dotcms.com
          |      |- images 
          |      |  |- file1
          |      |  |- file2
          |      |- application
          |         |- file3
          |         |- file4             
          |     
          |- es-CR 
             |- demo.dotcms.com (site)
                |- images 
                |  |- file1
                |  |- file2
                |- application
                   |- file3
                   |- file4
                   
</pre>


Please notice that content type file must end in **_.contentype.json_**
in a similar fashion site definition files must end in **_.site.json_** 
otherwise they will be ignored by the script.

## Usage

### Config

Before you can use the tool you need to make it aware of the location of the dotCMS instance 
and also get a valid API Access Key associated with an authorized user. 
[Here's](https://dotcms.com/docs/latest/authentication-using-jwt#APIAccessKeys) how you can get a valid API Access Key.
Aka (JSON_WEB_TOKEN)
Once you have such information at hand. it must be set in the config file located in: 
<pre>
tools/cli/config    
</pre>

The following properties are expected by the tool.
  
<pre>
DOTCMS_BASE_URL
JSON_WEB_TOKEN
USER
PASS (Optional)
</pre>

### Json Files

**Site**:

We're providing a basic example that takes only one simple parameter Name,
But you can pass much more. 

Further info is available in our API-Postman Test

./src/main/resources/osgi-bundle/Site Resource.postman_collection.json 


**Content-Type**:

The Content-Type Json files used by this script are the same definitions returned by our Content-Type API

Here are a few examples:

https://demo.dotcms.com/api/v1/contenttype/id/Product \
https://demo.dotcms.com/api/v1/contenttype/id/FileAsset \
https://demo.dotcms.com/api/v1/contenttype/id/Blog 

**Important**: The json returned by those endpoints must be slightly modified to fit the requirements of our API

Some of our APIs wrap the main response body around an additional `Entity` level. 
Therefore the main json body must be extracted before putting it into a _.contentype.json_ file

Another relevant detail is that content-type definition files must be fed with a valid site id. If not the content-type would be rejected.

We support pushing content-type file using site-name. Back in the server side the site-name will be resolved/  
e.g.

  <pre>
   "fixed":false,
   "folder":"SYSTEM_FOLDER",
   "host":"my.cool-bike.com",
   "iDate":1623251909000,
   "id":"eab1ad11292a6052b90a6247b122858b"
  </pre>    

if your bundle has a site named "my.cool-bike.com" that has been previously created such named can be used in you content-type files.
But host can also be a regular valid site identifier or name. If the host is an empty string the CT will be placed under System-Host

**Important**: When a site-name gets passed to the Rest API. if the API fails to resolve the site-name. The new Content-Type will be placed under System-Host site as a fall-back and no errors will be reported on the logs by the app.

### Commands

**_Import bundles_**: Takes a folder as parameter and it can be called by doing:
<pre>
  ./dot-cli --bundle ../cli/bundles/bike-shop  
</pre>
Basically this command takes care of loading the entire bundle into a remote instance of dotCMS

**But if you want to do it piece by piece there are also some other options**

**_Import files_**: Also takes a folder as parameter and it can be called by doing:
<pre>
./dot-cli --files ~/code/etc/bundle/files
</pre>
This command imports the file assets piece of the directory structure described above. 

**_Import sites_**: Takes a file as parameter like this:
<pre>
./dot-cli --site ../cli/bundles/bike-shop/sites/my.cool-bike.com.site.json
</pre>

**Important**: Make sure your content-type file has valid site identifier set when using this command. As interpolation here is of no use.

**_Import Content-Types_**: Takes a file as parameter like this:
<pre>
./dot-cli --content-type ../cli/bundles/bike-shop/content-types/bike.contenttype.json
</pre>