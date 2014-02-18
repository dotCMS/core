In order to deploy dotcms as a war in jboss 7 is required to:

    From a dotcms installation:
        1. Run the ant task: deploy-war-jboss7 running:
             ant deploy-war-jboss7

             * That task first will checkout jboss 7 (Version 7.1.1) from a git repository (https://github.com/dotCMS/jboss7.git) if
               it wasn't already checked out.

             * After check out the repository it will generate and move the dotcms war content to the jboss
               deployments folder (jboss7/standalone/deployments).

             * Jboss will be checked out by default in a folder called jboss7 a level up from the dotcms installation folder.
               The location of the jboss folder can be change modifying the file: /com/liferay/portal/util/build.properties

        2. Configure the jboss datasources for dotcms
        3. Run the bin/standalone.sh file on unix and for windows the bin/standalone.bat file
