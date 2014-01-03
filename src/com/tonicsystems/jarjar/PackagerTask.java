package com.tonicsystems.jarjar;

import com.tonicsystems.jarjar.ext_util.EntryStruct;
import com.tonicsystems.jarjar.resource.MatchableRule;
import com.tonicsystems.jarjar.resource.ResourceRewriter;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ZipFileSet;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Jonathan Gamba
 *         Date: 10/15/13
 */
public class PackagerTask extends JarJarTask {

    private String dotcmsHome;
    private String outputFolder;
    private String outputFile;
    private String dotcmsJar;
    private String dotVersion;
    private String onlyJar;
    private boolean multipleJars;

    private boolean generateRulesFromParentFolder;
    private boolean skipJarsGeneration;
    private boolean skipDependenciesReplacement;
    private boolean skipDependenciesJars;
    private boolean skipJpsFiles;

    private Vector<FileSet> filesets = new Vector<FileSet>();
    private List<Dependency> dependencies = new ArrayList<Dependency>();
    private List<NamingRule> namingRules = new ArrayList<NamingRule>();

    private boolean initialRenameServices;
    private boolean initialVerbose;

    public void execute () throws BuildException {

        //Track the time
        long startProcessTime = System.currentTimeMillis();

        log( "Executing Packager task....", Project.MSG_INFO );

        //Remember the initial values, they are clean after repackage each jar
        initialRenameServices = renameServices;
        initialVerbose = verbose;

        // Validate input
        if ( this.dotcmsJar == null ) {
            throw new IllegalArgumentException( "No dotCMS jar specified" );
        }
        File dotcmsJarFile = new File( this.dotcmsJar );
        log( "dotCMS jar location: " + dotcmsJarFile.getAbsolutePath(), Project.MSG_INFO );

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //FIRST WE RUN THE INSPECTOR TO FIND DUPLICATED CLASSES
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++

        //Create a formatter to read and display the results
        Formatter formatter = new PlainFormatter();
        formatter.setOutput( System.out );
        formatter.setDuplicatesOnly( true );

        //Create the inspector in order to analyze the given path
        Inspector inspector = new Inspector();
        inspector.addFormatter( formatter );

        Collection<File> toTransform = new ArrayList<File>();

        List<File> parentFolders = new ArrayList<File>();

        //Find all the jars define to repackage
        if ( filesets != null ) {
            for ( FileSet fileSet : filesets ) {

                if ( fileSet.getDirectoryScanner() != null && fileSet.getDirectoryScanner().getIncludedFiles() != null ) {
                    for ( String file : fileSet.getDirectoryScanner().getIncludedFiles() ) {

                        File fileToInspect = new File( fileSet.getDirectoryScanner().getBasedir().getAbsolutePath() + File.separator + file );

                        //Global list of jars to repackage
                        if ( !toTransform.contains( fileToInspect ) ) {
                            toTransform.add( fileToInspect );
                        }

                        if ( isGenerateRulesFromParentFolder() ) {
                            if ( !parentFolders.contains( fileToInspect.getParentFile() ) ) {
                                parentFolders.add( fileToInspect.getParentFile() );
                                inspector.inspect( fileToInspect.getParentFile() );
                            }
                        } else {
                            inspector.inspect( fileToInspect );
                        }
                    }
                }
            }
        }
        for ( Dependency dependency : dependencies ) {

            if ( dependency.isGenerate() ) {
                //The file to check and probably to modify
                File fileToInspect = new File( dependency.getPath() );
                inspector.inspect( fileToInspect );
            }
        }

        //inspector.report( "Duplicated classes" );//Generate a report

        log( "-----------------------------------------" );
        log( "Found " + inspector.getClassCount() + " unique classes" );
        log( "Found " + inspector.getDuplicateCount() + " duplicated classes" );

        /*if ( inspector.getDuplicateCount() > 0 ) {
            throw new IllegalStateException( "Found duplicated classes on the specified class path, you need to fix those duplicates " +
                    "in order to continue." );
        }*/

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //PREPARE ALL THE JARS AND RULES TO APPLY
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        HashMap<String, CustomRule> rulesToApply = new HashMap<String, CustomRule>();

        //Get all the classes we found
        createRules( inspector.getClasses(), rulesToApply );
        //Get all the resources we found
        createRules( inspector.getResources(), rulesToApply );

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //NOW REPACKAGE THE JARS
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++

        //-------------------------------
        //SOME LOGGING
        log( "-----------------------------------------" );
        log( "Jars to repackage: " + toTransform.size() );
        logRules( rulesToApply.values() );
        logJars( toTransform );

        if ( !isSkipJarsGeneration() ) {//This can be use for testing purposes

            //Track the time
            long startJarsGeneration = System.currentTimeMillis();

            //Repackage the jars but how we do it depends on if we want them all on a single jar or multiples
            if ( isMultipleJars() ) {

                //Repackage jar by jar
                for ( File jar : toTransform ) {

                    //This can be use for testing purposes
                    if ( getOnlyJar() != null && !getOnlyJar().isEmpty()
                            && !getOnlyJar().equals( jar.getName() ) ) {
                        continue;
                    }

                    //Repackaging this single jar
                    Collection<File> transform = new ArrayList<File>();
                    transform.add( jar );

                    File outJar = new File( getOutputFolder() + File.separator + jar.getName() );
                    generate( outJar, rulesToApply.values(), transform );
                }

            } else {
                //Repackage all the jars in a single jar
                File outJar = new File( getOutputFolder() + File.separator + getOutputFile() );
                generate( outJar, rulesToApply.values(), toTransform );
            }

            //Log the time
            trackTime( "Generated Jars!!", startJarsGeneration );
        }

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //APPLY THE SAME RULES TO GIVEN FILES,JAR's,  XML's, .PROPERTIES, ETC...
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++

        ResourceRewriter resourceRewriter = new ResourceRewriter( new CustomContentRewriter( rulesToApply.values() ), initialVerbose, initialRenameServices );

        //Add the dotcms jar as a dependency we need to check and modify
        Dependency dotcmsJarDependency = new Dependency();
        dotcmsJarDependency.setPath( this.dotcmsJar );
        dependencies.add( dotcmsJarDependency );

        //Track the time
        long startDependenciesChanges = System.currentTimeMillis();

        for ( Dependency dependency : dependencies ) {

            //The file to check and probably to modify
            String filePath = dependency.getPath();

            //Verify if we are going to check for dependencies on another jars or files
            if ( isJarName( filePath ) ) {

                if ( !isSkipDependenciesJars() ) {//This can be use for testing purposes

                    //This can be use for testing purposes
                    File jarToVerify = new File( filePath );
                    if ( getOnlyJar() != null && !getOnlyJar().isEmpty()
                            && !getOnlyJar().equals( jarToVerify.getName() ) ) {
                        continue;
                    }

                    if ( dependency.isGenerate() ) {

                        //Repackaging this single jar
                        File jar = new File( filePath );

                        Collection<File> transform = new ArrayList<File>();
                        transform.add( jar );

                        File outJar = new File( getOutputFolder() + File.separator + jar.getName() );
                        generate( outJar, rulesToApply.values(), transform, dependency.isRenameServices() );

                    } else {
                        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
                        //CHANGE THE OLD REFERENCES IN DEPENDENT JARS
                        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
                        repackageDependent( filePath, rulesToApply );
                    }
                }

            } else {

                if ( !isSkipDependenciesReplacement() ) {//This can be use for testing purposes

                    try {

                        EntryStruct struct = new EntryStruct();

                        //Reading the file to check
                        FileInputStream inputStream = new FileInputStream( filePath );
                        struct.data = IOUtils.toByteArray( inputStream );
                        struct.name = filePath;
                        struct.time = new Date().getTime();

                        //Apply the rules to the given structure
                        resourceRewriter.process( struct );

                        //Apply the results to the file
                        FileOutputStream outputStream = new FileOutputStream( new File( filePath ) );
                        outputStream.write( struct.data );
                        outputStream.close();

                    } catch ( FileNotFoundException e ) {
                        log( "File " + filePath + " not found.", e, Project.MSG_ERR );
                        //throw new BuildException( "File " + filePath + " not found.", e );
                    } catch ( IOException e ) {
                        log( "Error checking and/or modifying " + filePath + ".", e, Project.MSG_ERR );
                        //throw new BuildException( "Error checking and/or modifying " + filePath + ".", e );
                    }

                }
            }
        }

        //Log the time
        trackTime( "Changes applied to Dependencies!!", startDependenciesChanges );

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //APPLY THE SAME RULES TO JSP's UNDER THE DOTCMS FOLDER
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        long startJSPsChanges = System.currentTimeMillis();//Track the time
        File jspFolderFile = new File( getDotcmsHome() + File.separator + "dotCMS" );
        File srcFolderFile = new File( getDotcmsHome() + File.separator + "src" );
        String searchOn = jspFolderFile.getAbsolutePath() + " " + srcFolderFile.getAbsolutePath();

        log( "" );
        log( "-----------------------------------------" );
        log( "Updating JSP's and java source code" );

        /*
         The faster way to replacing content on the jsp files is for sure executing shell commands.
         The combination of find and perl do the job, but in order to make it work more efficiently we need to use the multiple replace ('s/op1/np1/g;s/op2/np2/g;...'),
          replacing rule by rule is very slow.
         BTW, we must use chunks of rules to replace, sending all of them at the same time will cause a nice "java.io.IOException: error=7, Argument list too long"
         */
        int chunks = 200;
        int i = 0;
        StringBuilder searchPatters = new StringBuilder();

        /*
         We will keep in a different string builder the commands to apply and will write them in a bash file in case
         that someone needs to apply these rules to external folders and/or files.
         */
        StringBuilder replaceCommands = new StringBuilder();
        replaceCommands.append( "folder=\"" ).append( jspFolderFile.getAbsolutePath() ).append( "\"" ).append( "\n" );

        for ( CustomRule rule : rulesToApply.values() ) {

            //Clean-up the pattern
            String pattern = rule.getPattern();
            pattern = pattern.replaceAll( "\\*", "" );
            String patternFinal = pattern.replaceAll( "\\.", "\\\\." );

            //Clean up the result
            String result = rule.getResult();
            if ( result.lastIndexOf( "@" ) != -1 ) {
                result = result.substring( 0, result.lastIndexOf( "@" ) );
            }
            String resultFinal = result.replaceAll( "\\.", "\\\\." );

            if ( searchPatters.length() > 0 ) {
                searchPatters.append( ";" );
            }
            searchPatters.append( "s/((?<=\")|(?<=\\()|(?<=,)|(?<=\\s))" ).append( patternFinal ).append( "/" ).append( resultFinal ).append( "/g" );
            i++;

            if ( i == chunks ) {
                /*
                 Executes the replacing command on all the jsp files under the given folder, also adds the this used command to
                 the string builder with the total of executed commands for a possible manual use.
                 */
                sourcesReplacement( searchOn, searchPatters, replaceCommands );
                i = 0;
                searchPatters = new StringBuilder();
            }
        }

        if ( i > 0 ) {
            /*
             Executes the replacing command on all the jsp files under the given folder, also adds the this used command to
             the string builder with the total of executed commands for a possible manual use.
             */
            sourcesReplacement( searchOn, searchPatters, replaceCommands );
        }

        //Log the time
        trackTime( "Changes applied to JSPs!!", startJSPsChanges );

        /*
         We already have the commands to apply to jsp and java files, now lets create the commands for the
         remaining resources: xml, xsd, tld, properties, etc...
         After collecting all this commands we will write them in a file and will be ready for "migrate" external content.
         ALSO a script for "undo" modified resources with repackaged packages will be created.
         */
        StringBuilder undoCommands = new StringBuilder();
        generateResourcesCommands( rulesToApply.values(), replaceCommands, undoCommands );

        //Write this list of commands into a bash file
        writeToLog( replaceCommands, "replace.sh" );
        writeToLog( undoCommands, "undo.sh" );

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //Log the total time
        trackTime( "Finished Process!!", startProcessTime );
    }

    /**
     * Creates the rules for repackaging based on a list of given resources
     *
     * @param resources
     * @param rulesToApply
     */
    private void createRules ( HashMap<String, List<Inspector.PathInfo>> resources, HashMap<String, CustomRule> rulesToApply ) {

        for ( String name : resources.keySet() ) {

            //Collection with the info of found for each class
            List<Inspector.PathInfo> details = resources.get( name );
            for ( Inspector.PathInfo detail : details ) {//Duplicated classes as present in multiple jars
                File jarFile = detail.base;

                //Handle some strings to use in the rules
                String packageName;
                if ( name.lastIndexOf( "." ) != -1 ) {
                    //Get the package of the class (from my.package.myClassName to my.package)
                    packageName = name.substring( 0, name.lastIndexOf( "." ) );

                    if ( !packageName.contains( "." ) && (
                            packageName.equals( "common" )
                                    || packageName.equals( "schema" )
                                    || packageName.equals( "conf" )
                                    || packageName.equals( "images" )
                                    || packageName.equals( "config" )) ) {
                        /*
                          We don't want something like common.**, a very small an common package, replacing this is dangerous
                         */
                        continue;
                    } else if ( packageName.contains( "META-INF" ) ) {
                        continue;
                    }
                } else {
                    //On the root??, no package??
                    continue;
                }

                //Handle the packages we marked to exclude by jar
                Boolean ignorePackage = ignorePackage( jarFile, packageName );
                if ( ignorePackage ) {
                    continue;
                }

                //Create a name to be part of the resulting package name
                String jarNameForPackage = jarFile.getName().substring( 0, jarFile.getName().lastIndexOf( "." ) );
                //String jarNameForPackage = "_" + getDotVersion() + "_";
                jarNameForPackage = jarNameForPackage.replaceAll( "-", "_" );
                jarNameForPackage = jarNameForPackage.replaceAll( "\\.", "_" );
                jarNameForPackage = jarNameForPackage.toLowerCase();

                /*
                 Verify if we have some explicit naming rules for this jar.
                 How it works:
                    Example: all the jars that contains the word jersey (jersey-client-1.12.jar, jersey-core-1.12.jar, jersey-json-1.12.jar, etc)
                    will share the same repackaged name "jersey_1_12" --> com.dot.repackage.jersey_1_12
                 */
                for ( NamingRule namingRule : namingRules ) {
                    if ( jarNameForPackage.contains( namingRule.getPattern() ) ) {
                        jarNameForPackage = namingRule.getReplacement();
                        break;
                    }
                }

                //Create the rule for this class and add it to the list of rules for this jar
                String pattern = packageName + ".*";//Example: "org.apache.xerces.dom.*"
                if ( packageName.equals( "org.elasticsearch.common.joda.time.tz" ) ) {//For this package we can not be so strict as is not contains .class files but instead a lot of resources
                    pattern = packageName + ".**";//Example: "org.apache.xerces.dom.**"
                }
                String result = "com.dotcms.repackage." + jarNameForPackage + "." + packageName + ".@1";

                CustomRule rule = new CustomRule();
                rule.setPattern( pattern );
                rule.setResult( result );
                rule.setParent( jarFile.getName() );

                //Global list of rules to apply
                if ( !rulesToApply.containsKey( pattern + result ) ) {
                    rulesToApply.put( pattern + result, rule );
                }

            }
        }
    }

    /**
     * Finds and replaces in given folders a list of packages using regex
     *
     * @param folders
     * @param searchPatters
     */
    private void sourcesReplacement ( String folders, StringBuilder searchPatters, StringBuilder commands ) {

        //find . -name '*.jsp' -exec perl -pi -e 's/((?<=")|(?<=\()|\s)org\.apache\.struts\.taglib\.tiles\./com\.dotcms\.repackage\.org\.apache\.struts\.taglib\.tiles\./' {} \;

        //String command = "find " + jspFolderFile.getAbsolutePath() + " -name '*.jsp' -exec sed -i 's/\"" + pattern + "/\"" + result + "/' {} \\;";
        //String command = "find " + jspFolderFile.getAbsolutePath() + " -name '*.jsp' -exec perl -pi -e 's/((?<=\")|(?<=\\()|\\s)" + patternFinal + "/" + resultFinal + "/g' {} \\;";
        //String command = "find " + jspFolderFile.getAbsolutePath() + " -name '*.jsp' -exec perl -pi -e '" + searchPatters + "' {} \\;";
        String command = "find " + folders + " \\( -name '*.jsp' -o -name '*.java' \\) -exec perl -pi -e '" + searchPatters.toString() + "' {} +";
        log( command );

        String externalCommand = "find $folder \\( -name '*.jsp' -o -name '*.java' \\) -exec perl -pi -e '" + searchPatters.toString() + "' {} +";
        commands.append( externalCommand ).append( "\n" );

        if ( !isSkipJpsFiles() ) {//This can be use for testing purposes

            try {
                Process process = Runtime.getRuntime().exec(
                        new String[]{
                                "sh", "-l", "-c", command
                        }
                );
                process.waitFor();
            } catch ( Exception e ) {
                log( "Error replacing JSP's Strings: " + searchPatters.toString() + ".", e, Project.MSG_ERR );
            }
        }
    }

    /**
     * Generates the commands required to build a script able to replace old packages to new ones (repackaged packages)
     * and the commands required to build a script able to replace new packages (repackaged packages) to old one.
     *
     * @param rulesToApply
     * @param replaceCommands
     * @param undoCommands
     */
    private void generateResourcesCommands ( Collection<CustomRule> rulesToApply, StringBuilder replaceCommands, StringBuilder undoCommands ) {

        //Folders where to search resources to undo
        String jspDir = getDotcmsHome() + File.separator + "dotCMS";
        String srcConfDir = getDotcmsHome() + File.separator + "src-conf";
        String buildLibsDir = getDotcmsHome() + File.separator + "libs" + File.separator + "buildlibs";
        undoCommands.append( "folder=\"" ).append( jspDir ).append( " " ).append( srcConfDir ).append( " " ).append( buildLibsDir ).append( "\"" ).append( "\n" );

        String fileTypes = "-name '*.xml' -o -name '*.xsd' -o -name '*.tld' -o -name '*.properties' -o -name '*.conf' -o -name '*.txt' -o -name '*.MF'";

        int chunks = 100;
        int i = 0;
        StringBuilder searchReplacePatters = new StringBuilder();
        StringBuilder searchUndoPatters = new StringBuilder();
        for ( CustomRule rule : rulesToApply ) {

            /*
             -----------------------------------------------------------------
             Prepare the commands for replacement
             */
            String currentPattern = rule.getPattern();

            Boolean strict = true;
            if ( currentPattern.contains( ".**" ) ) {
                currentPattern = currentPattern.replace( ".**", ".*" );
            }

            //Clean-up the pattern
            String packagePattern = MatchableRule.PackagePattern( currentPattern, strict );
            String pathPattern = MatchableRule.PathPattern( currentPattern, strict );
            //Clean up the result
            String packageResult = MatchableRule.PackageReplacement( rule.getResult(), strict );
            String pathResult = MatchableRule.PathReplacement( rule.getResult(), strict );

            if ( searchReplacePatters.length() > 0 ) {
                searchReplacePatters.append( ";" );
            }
            searchReplacePatters.append( "s{" ).append( packagePattern ).append( "}{" ).append( packageResult ).append( "}g;s{" ).append( pathPattern ).append( "}{" ).append( pathResult ).append( "}g" );

            /*
             -----------------------------------------------------------------
             Prepare the commands for undo
             */
            packagePattern = currentPattern.replace( ".*", "" );
            packagePattern = packagePattern.replaceAll( "\\.", "\\\\." );
            pathPattern = packagePattern.replaceAll( "\\.", "/" );

            packageResult = rule.getResult().replace( ".@1", "" );
            packageResult = packageResult.replaceAll( "\\.", "\\\\." );
            pathResult = packageResult.replaceAll( "\\.", "/" );

            if ( searchUndoPatters.length() > 0 ) {
                searchUndoPatters.append( ";" );
            }
            searchUndoPatters.append( "s{" ).append( packageResult ).append( "}{" ).append( packagePattern ).append( "}g;s{" ).append( pathResult ).append( "}{" ).append( pathPattern ).append( "}g" );

            i++;
            if ( i == chunks ) {

                //Replacements
                String externalCommand = "find $folder \\( " + fileTypes + " \\) -exec perl -pi -e '" + searchReplacePatters.toString() + "' {} +";
                replaceCommands.append( externalCommand ).append( "\n" );
                //Undo
                externalCommand = "find $folder \\( -name '*.jsp' -o " + fileTypes + " \\) -exec perl -pi -e '" + searchUndoPatters.toString() + "' {} +";
                undoCommands.append( externalCommand ).append( "\n" );

                i = 0;
                searchReplacePatters = new StringBuilder();
                searchUndoPatters = new StringBuilder();
            }
        }

        if ( i > 0 ) {
            //Replacements
            String externalCommand = "find $folder \\( " + fileTypes + " \\) -exec perl -pi -e '" + searchReplacePatters.toString() + "' {} +";
            replaceCommands.append( externalCommand ).append( "\n" );
            //Undo
            externalCommand = "find $folder \\( -name '*.jsp' -o " + fileTypes + " \\) -exec perl -pi -e '" + searchUndoPatters.toString() + "' {} +";
            undoCommands.append( externalCommand ).append( "\n" );
        }
    }

    /**
     * Repackage a given jar that was using some of the packages we changed
     *
     * @param jarPath
     * @param rulesToApply
     */
    private void repackageDependent ( String jarPath, HashMap<String, CustomRule> rulesToApply ) {

        File jarFile = new File( jarPath );
        String jarName = jarFile.getName().substring( 0, jarFile.getName().lastIndexOf( "." ) );
        //String jarLocation = jarFile.getAbsolutePath().substring( 0, jarFile.getAbsolutePath().lastIndexOf( File.separator ) );

        //Prepare the jars that we are going to repackage
        Collection<File> files = new ArrayList<File>();
        files.add( jarFile );
        //-------------------------------
        //SOME LOGGING
        logJars( files );

        //Repackage the jar into a temporal file
        String tempJarName = jarName + "_temp" + ".jar";
        File tempJar = new File( getOutputFolder() + File.separator + tempJarName );
        generate( tempJar, rulesToApply.values(), files );

        /*
         Rename the just repackaged temporal jar
         */
        //If the jars live in the same folder delete the original jar in order to be able to rename the temporal file
        if ( jarFile.getParent().equals( tempJar.getParent() ) ) {
            //Remove the original jar
            jarFile.delete();
        }

        File finalJar = new File( tempJar.getParent() + File.separator + jarFile.getName() );
        tempJar.renameTo( finalJar );
    }

    /**
     * Repackage a list of given jars applying given rules
     *
     * @param outFile the output jar after processing
     * @param rules   rules to apply
     * @param jars    jars to integrate into one
     */
    private void generate ( File outFile, Collection<CustomRule> rules, Collection<File> jars ) {
        generate( outFile, rules, jars, initialRenameServices );
    }

    /**
     * Repackage a list of given jars applying given rules
     *
     * @param outFile        the output jar after processing
     * @param rules          rules to apply
     * @param jars           jars to integrate into one
     * @param renameServices rename the services files
     */
    private void generate ( File outFile, Collection<CustomRule> rules, Collection<File> jars, boolean renameServices ) {

        //First lets sort the rules collection, first the rules created from this jar
        List<CustomRule> rulesList = new ArrayList<CustomRule>( rules );
        Collections.sort( rulesList, new CustomRule().new RuleSortByParent( outFile.getName() ) );

        //Destiny file
        setDestFile( outFile );

        //Prepare the jars that we are going to repackage
        for ( File jar : jars ) {

            ZipFileSet fileSet = new ZipFileSet();
            fileSet.setSrc( jar );

            //Add it to the fileset
            addZipfileset( fileSet );
        }

        //Prepare the rules for these groups of jar
        List<PatternElement> patterns = new ArrayList<PatternElement>();
        for ( CustomRule rule : rulesList ) {

            if ( jars.size() == 1 ) {
                //Handle the packages we marked to exclude by jar
                Boolean ignorePackage = ignorePackage( jars.iterator().next(), rule.getPattern() );
                if ( ignorePackage ) {
                    continue;
                }
            }

            addConfiguredRule( rule );
            patterns.add( rule );
        }

        //Generate the new jar
        MainProcessor processor = new MainProcessor( patterns, initialVerbose, false, renameServices );
        execute( processor );
        try {
            processor.strip( getDestFile() );
        } catch ( IOException e ) {
            throw new BuildException( e );
        }
        //super.execute();

        //Clean everything and get ready to start again
        super.reset();
    }

    /**
     * Verify if a package was marked to be exclude on this jar
     *
     * @param jarFile
     * @param packageName
     * @return
     */
    private Boolean ignorePackage ( File jarFile, String packageName ) {

        packageName = packageName.replace( ".**", "" );
        packageName = packageName.replace( ".*", "" );

        //Handle the packages we marked to exclude by jar
        Boolean ignorePackage = false;
        for ( Dependency dependency : dependencies ) {

            //Verify if we have packages to ignore on this dependency
            List<Dependency.Ignore> toIgnore = dependency.getPackagesToIgnore();
            if ( toIgnore != null && !toIgnore.isEmpty() ) {

                for ( Dependency.Ignore ignore : toIgnore ) {
                    File owner = new File( dependency.getPath() );
                    if ( packageName.startsWith( ignore.getParentPackage() ) && jarFile.getName().equals( owner.getName() ) ) {
                        ignorePackage = true;
                        break;
                    }
                }
            }

            if ( ignorePackage ) {
                break;
            }
        }

        return ignorePackage;
    }

    /**
     * Tracks processing times
     *
     * @param title
     * @param startProcessTime
     */
    private void trackTime ( String title, long startProcessTime ) {

        long milliseconds = System.currentTimeMillis() - startProcessTime;
        DateFormat dateFormat = new SimpleDateFormat( "HH:mm:ss" );
        dateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );

        Date date = new Date( milliseconds );
        String formattedDate = dateFormat.format( date );

        log( "" );
        log( "" );
        log( "-------------------------------------------------------" );
        log( "-------------------------------------------------------" );
        log( title + " - Elapsed time: " + formattedDate );
        log( "-------------------------------------------------------" );
        log( "-------------------------------------------------------" );
    }

    private void logJars ( Collection<File> jars ) {

        //-------------------------------
        //SOME LOGGING
        log( "" );
        log( "-----------------------------------------" );
        log( "Repackaging... " );
        log( "" );
        for ( File jar : jars ) {
            log( jar.getName() );
        }
        log( "" );
        //-------------------------------
    }

    private void logRules ( Collection<CustomRule> rules ) {

        StringBuilder rulesBuilder = new StringBuilder();

        //SOME LOGGING
        log( "" );
        log( "-----------------------------------------" );
        log( "Rules to apply: " );
        for ( CustomRule rule : rules ) {
            rulesBuilder.append( rule.getPattern() ).append( "-->" ).append( rule.getResult() ).append( "\n" );
            log( rule.getPattern() + " --> " + rule.getResult() );
        }

        //Write this list of rules into a log file
        writeToLog( rulesBuilder, "rules.log" );
    }

    /**
     * Writes a given content inside file
     *
     * @param bob
     * @param logName
     */
    private void writeToLog ( StringBuilder bob, String logName ) {

        String baseDir = this.getProject().getBaseDir().getAbsolutePath();
        File outputFile = new File( baseDir + File.separator + logName );
        if ( outputFile.exists() ) {
            outputFile.delete();
        }

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( outputFile ), "utf-8" ) );
            writer.write( bob.toString() );
        } catch ( IOException ex ) {
            log( ex.getMessage(), ex, Project.MSG_ERR );
        } finally {
            try {
                if ( writer != null ) {
                    writer.close();
                }
            } catch ( Exception ex ) {
                log( ex.getMessage(), ex, Project.MSG_ERR );
            }
        }
    }

    /**
     * Return true if the name corresponds to a jar file
     *
     * @param name The file name
     * @return true if the name is a jar file, false otherwise.
     */
    private boolean isJarName ( String name ) {
        name = name.toLowerCase();
        return name.endsWith( ".jar" );
    }

    public String getDotVersion () {
        return dotVersion;
    }

    public void setDotVersion ( String dotVersion ) {
        this.dotVersion = dotVersion;
    }

    public String getOnlyJar () {
        return onlyJar;
    }

    public void setOnlyJar ( String onlyJar ) {
        this.onlyJar = onlyJar;
    }

    public String getDotcmsHome () {
        return dotcmsHome;
    }

    public void setDotcmsHome ( String dotcmsHome ) {
        this.dotcmsHome = dotcmsHome;
    }

    public String getOutputFolder () {
        return outputFolder;
    }

    public void setOutputFolder ( String outputFolder ) {
        this.outputFolder = outputFolder;
    }

    public String getOutputFile () {
        return outputFile;
    }

    public void setOutputFile ( String outputFile ) {
        this.outputFile = outputFile;
    }

    public String getDotcmsJar () {
        return dotcmsJar;
    }

    public void setDotcmsJar ( String dotcmsJar ) {
        this.dotcmsJar = dotcmsJar;
    }

    public boolean isMultipleJars () {
        return multipleJars;
    }

    public void setMultipleJars ( boolean multipleJars ) {
        this.multipleJars = multipleJars;
    }

    public boolean isGenerateRulesFromParentFolder () {
        return generateRulesFromParentFolder;
    }

    public void setGenerateRulesFromParentFolder ( boolean generateRulesFromParentFolder ) {
        this.generateRulesFromParentFolder = generateRulesFromParentFolder;
    }

    public boolean isSkipDependenciesReplacement () {
        return skipDependenciesReplacement;
    }

    public void setSkipDependenciesReplacement ( boolean skipDependenciesReplacement ) {
        this.skipDependenciesReplacement = skipDependenciesReplacement;
    }

    public boolean isSkipDependenciesJars () {
        return skipDependenciesJars;
    }

    public void setSkipDependenciesJars ( boolean skipDependenciesJars ) {
        this.skipDependenciesJars = skipDependenciesJars;
    }

    public boolean isSkipJarsGeneration () {
        return skipJarsGeneration;
    }

    public void setSkipJarsGeneration ( boolean skipJarsGeneration ) {
        this.skipJarsGeneration = skipJarsGeneration;
    }

    public boolean isSkipJpsFiles () {
        return skipJpsFiles;
    }

    public void setSkipJpsFiles ( boolean skipJpsFiles ) {
        this.skipJpsFiles = skipJpsFiles;
    }

    public void addFileset ( FileSet fileset ) {
        filesets.add( fileset );
    }

    public NamingRule createNamingRule () {
        NamingRule namingRule = new NamingRule();
        namingRules.add( namingRule );

        return namingRule;
    }

    public Dependency createDependency () {
        Dependency dependency = new Dependency();
        dependencies.add( dependency );

        return dependency;
    }

}