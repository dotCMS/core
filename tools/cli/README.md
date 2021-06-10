
# dot-cli

This is a Bash Command Line Interface (CLI) to interact with dotCMS

It allows you to create update ContentTypes, Sites, Folder, and File Assets
 

https://github.com/dotCMS/core

## Pre Requisites

This CLI relies on 
[jq](https://stedolan.github.io/jq)
Find a suitable version or the instructions to get it installed via package manager here:
[here](https://stedolan.github.io/jq/download/)

## Usage

dotCMS command line interface can used as follows

The tool expects a directory like structure holding all the json descriptor/files to be pushed 

the expected directory structure looks like this:

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


## Usage:

Import bundles takes as parameter a folder and it can be called by doing:
<pre>
  ./dot-cli --bundle ~/code/etc/bundle  
</pre>

Import sites Takes a  can be accomplished by doing:
