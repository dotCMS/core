In order to deploy dotcms as a war in tomcat 7 is required to:

    From a dotcms installation:
        1. Configure the database setting modifying the file: /extra/tomcat-7/context.xml
        2. Run the ant task: deploy-war-tomcat7 running:
             ant deploy-war-tomcat7

             * That task first will checkout tomcat 7 (Version 7.0.42) from a git repository (https://github.com/dotCMS/tomcat7.git) if
               it wasn't already checked out.

             * After check out the repository it will generate and move the dotcms war content to the tomcat webapps
               folder (tomcat7/webapps).

             * Tomcat will be checked out by default in a folder called tomcat7 a level up from the dotcms installation folder.
               The location of the tomcat folder can be change modifying the file: /com/liferay/portal/util/build.properties

        3. Modify the tomcat7/conf/server.xml if required to change the port where tomcat will run, by default it is the 80 port.
        4. Run the bin/dotStartup.sh file on unix and for windows the bin/dotStartup.bat file
        5. To stop the application run the file: bin/dotShutdown.sh on unix and for windows bin/dotShutdown.bat
