package com.tonicsystems.jarjar;

import com.tonicsystems.jarjar.ext_util.EntryStruct;
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

    private String outputFolder;
    private String outputFile;
    private String dotcmsJar;
    private String jspFolder;
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
        HashMap<String, Rule> rulesToApply = new HashMap<String, Rule>();

        //Get all the classes we found
        HashMap<String, List<Inspector.PathInfo>> classes = inspector.getClasses();
        for ( String name : classes.keySet() ) {

            //Collection with the info of found for each class
            List<Inspector.PathInfo> details = classes.get( name );
            Inspector.PathInfo detail = details.get( 0 );//Grabbing the first one, could be more than one for duplicated classes
            File jarFile = detail.base;

            //Handle some strings to use in the rules
            String packageName;
            if ( name.lastIndexOf( "." ) != -1 ) {
                //Get the package of the class (from my.package.myClassName to my.package)
                packageName = name.substring( 0, name.lastIndexOf( "." ) );

                if ( !packageName.contains( "." ) && packageName.equals( "common" ) ) {
                    /*
                     FIXME: We don't want something like common.**, a very small an common package, replacing this is dangerous
                     */
                    continue;
                }
            } else {
                //On the root??, no package??
                continue;
            }

            //Create a name to be part of the resulting package name
            String jarNameForPackage = jarFile.getName().substring( 0, jarFile.getName().lastIndexOf( "." ) );
            //String jarNameForPackage = "_" + getDotVersion() + "_";
            jarNameForPackage = jarNameForPackage.replaceAll( "-", "_" );
            jarNameForPackage = jarNameForPackage.replaceAll( "\\.", "_" );
            jarNameForPackage = jarNameForPackage.toLowerCase();

            //Create the rule for this class and add it to the list of rules for this jar
            String pattern = packageName + ".**";//Example: "org.apache.xerces.dom.**"
            String result = "com.dotcms.repackage." + jarNameForPackage + "." + packageName + ".@1";

            Rule rule = new Rule();
            rule.setPattern( pattern );
            rule.setResult( result );

            //Global list of rules to apply
            if ( !rulesToApply.containsKey( pattern + result ) ) {
                rulesToApply.put( pattern + result, rule );
            }
        }

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

        if ( !isSkipJpsFiles() ) {//This can be use for testing purposes

            //Track the time
            long startJSPsChanges = System.currentTimeMillis();

            //++++++++++++++++++++++++++++++++++++++++++++++++++++++
            //APPLY THE SAME RULES TO JSP's UNDER THE DOTCMS FOLDER
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++
            File jspFolderFile = new File( getJspFolder() );

            log( "" );
            log( "-----------------------------------------" );
            log( "Updating JSP's" );

            /*
             The faster way to replacing content on the jsp files is for sure executing shell commands.
             The combination of find and perl do the job, but in order to make it work more efficiently we need to use the multiple replace ('s/op1/np1/g;s/op2/np2/g;...'),
              replacing rule by rule is very slow.
             BTW, we must use chunks of rules to replace, sending all of them with cause a nice "java.io.IOException: error=7, Argument list too long"
             */
            int chunks = 200;
            int i = 0;
            String searchPatters = "";
            for ( Rule rule : rulesToApply.values() ) {

                //Clean-up the pattern
                String pattern = rule.getPattern();
                pattern = pattern.replaceAll( "\\*\\*", "" );
                String patternFinal = pattern.replaceAll( "\\.", "\\\\." );

                //Clean up the result
                String result = rule.getResult();
                if ( result.lastIndexOf( "@" ) != -1 ) {
                    result = result.substring( 0, result.lastIndexOf( "@" ) );
                }
                String resultFinal = result.replaceAll( "\\.", "\\\\." );

                if ( !searchPatters.isEmpty() ) {
                    searchPatters += ";";
                }
                searchPatters += "s/((?<=\")|(?<=\\()|(?<=,)|\\s)" + patternFinal + "/" + resultFinal + "/g";
                i++;

                if ( i == chunks ) {
                    executeCommand( jspFolderFile, searchPatters );
                    i = 0;
                    searchPatters = "";
                }
            }

            if ( i > 0 ) {
                executeCommand( jspFolderFile, searchPatters );
            }

            //Log the time
            trackTime( "Changes applied to JSPs!!", startJSPsChanges );
        }

        //Log the time
        trackTime( "Finished Process!!", startProcessTime );
    }

    /**
     * Finds and replaces in a given jsp folder a list of packages using regex
     *
     * @param jspFolderFile
     * @param searchPatters
     */
    private void executeCommand ( File jspFolderFile, String searchPatters ) {

        //find . -name '*.jsp' -exec perl -pi -e 's/((?<=")|(?<=\()|\s)org\.apache\.struts\.taglib\.tiles\./com\.dotcms\.repackage\.org\.apache\.struts\.taglib\.tiles\./' {} \;

        //String command = "find " + jspFolderFile.getAbsolutePath() + " -name '*.jsp' -exec sed -i 's/\"" + pattern + "/\"" + result + "/' {} \\;";
        //String command = "find " + jspFolderFile.getAbsolutePath() + " -name '*.jsp' -exec perl -pi -e 's/((?<=\")|(?<=\\()|\\s)" + patternFinal + "/" + resultFinal + "/g' {} \\;";
        //String command = "find " + jspFolderFile.getAbsolutePath() + " -name '*.jsp' -exec perl -pi -e '" + searchPatters + "' {} \\;";
        String command = "find " + jspFolderFile.getAbsolutePath() + " -name '*.jsp' -exec perl -pi -e '" + searchPatters + "' {} +";
        log( command );

        try {
            Process process = Runtime.getRuntime().exec(
                    new String[]{
                            "sh", "-l", "-c", command
                    }
            );
            process.waitFor();
        } catch ( Exception e ) {
            log( "Error replacing JSP's Strings: " + searchPatters + ".", e, Project.MSG_ERR );
            //throw new BuildException( "Error replacing JSP's Strings: " + pattern + ".", e );
        }
    }

    /**
     * Repackage a given jar that was using some of the packages we changed
     *
     * @param jarPath
     * @param rulesToApply
     */
    private void repackageDependent ( String jarPath, HashMap<String, Rule> rulesToApply ) {

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
    private void generate ( File outFile, Collection<Rule> rules, Collection<File> jars ) {
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
    private void generate ( File outFile, Collection<Rule> rules, Collection<File> jars, boolean renameServices ) {

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
        for ( Rule rule : rules ) {
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
        for ( File jar : jars ) {
            log( jar.getName() );
        }
        log( "" );
        //-------------------------------
    }

    private void logRules ( Collection<Rule> rules ) {

        //-------------------------------
        //SOME LOGGING
        log( "" );
        log( "-----------------------------------------" );
        log( "Rules to apply: " );
        for ( Rule rule : rules ) {
            log( rule.getPattern() + " --> " + rule.getResult() );
        }
        //-------------------------------
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

    public String getJspFolder () {
        return jspFolder;
    }

    public void setJspFolder ( String jspFolder ) {
        this.jspFolder = jspFolder;
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

    public Dependency createDependency () {
        Dependency dependency = new Dependency();
        dependencies.add( dependency );

        return dependency;
    }

}