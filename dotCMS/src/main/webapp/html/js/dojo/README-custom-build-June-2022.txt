The source of the current build with the config files are under the folder core/dotCMS/src/main/webapp/html/js/dojo/src

**Steps to Build a Custom Build.**

1. Download the desired Dojo version and place it in  core/dotCMS/src/main/webapp/html/js/dojo/src, currently Dotcms support 1.x version.
2. Once unzipped in the version folder, create a dojoConfig.js file at the same level of the “dojo” folder. As reference check the one in core/dotCMS/src/main/webapp/html/js/dojo/src.
3. Create a “build” folder at the same level of the “dojo” folder, create a build.js inside the new folder as reference use the one in core/dotCMS/src/main/webapp/html/js/dojo/src.
4. Copy the folder dotCMS/src/main/webapp/html/js/dojo/custom-build/dijit/themes/dmundra and paste it in dotCMS/src/main/webapp/html/js/dojo/src/<newversion>/dijit/themes/dmundra
5. Clear the content in the folder core/dotCMS/src/main/webapp/html/js/dojo/custom-buid.
6. Under the desired Dojo version, run the command: node dojo/dojo.js load=build --profile ./dojoConfig.js --release
7. Done, now the custom build is under the folder core/dotCMS/src/main/webapp/html/js/dojo/custom-buid
8. Once the update is successfully applied, create zip of the folder and put it in the folder core/dotCMS/src/main/webapp/html/js/dojo/src, in case we need to generate this in the future.
