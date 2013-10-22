package com.dotcms.packager;

import com.tonicsystems.jarjar.JarJarTask;
import com.tonicsystems.jarjar.Rule;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ZipFileSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author Jonathan Gamba
 *         Date: 10/15/13
 */
public class PackagerTask extends JarJarTask {

    private String libraryPath;
    private String outputFolder;
    private String outputFile;
    private String dotcmsJar;

    public void execute () throws BuildException {

        log( "Executing Packager task....", Project.MSG_INFO );

        // Validate input
        if ( this.libraryPath == null ) {
            throw new IllegalArgumentException( "No path element to inspect!" );
        }
        if ( this.dotcmsJar == null ) {
            throw new IllegalArgumentException( "No dotCMS jar specified" );
        }
        File dotcmsJar = new File( this.dotcmsJar );
        log( "dotCMS jar location: " + dotcmsJar.getAbsolutePath(), Project.MSG_INFO );

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //FIRST WE RUN THE INSPECTOR TO FIND DUPLICATED CLASSES
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        File libFolder = new File( this.libraryPath );
        log( "Reading from: " + libFolder.getAbsolutePath(), Project.MSG_INFO );

        //Create a formatter to read and display the results
        Formatter formatter = new PlainFormatter();
        formatter.setOutput( System.out );
        formatter.setDuplicatesOnly( true );

        //Create the inspector in order to analyze the given path
        Inspector inspector = new Inspector();
        inspector.addFormatter( formatter );
        inspector.inspect( libFolder );
        //inspector.report( "Duplicated classes" );//Generate a report

        log( "-----------------------------------------" );
        log( "Found " + inspector.getClassCount() + " unique classes" );
        log( "Found " + inspector.getDuplicateCount() + " duplicated classes" );

        if ( inspector.getDuplicateCount() > 0 ) {
            throw new IllegalStateException( "Found duplicated classes on the specified class path, you need to fix those duplicates " +
                    "in order to continue." );
        }

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //PREPARE ALL THE JARS AND RULES TO APPLY
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++

        Collection<File> toTransform = new ArrayList<File>();
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
            } else {
                //On the root??, no package??
                continue;
            }

            //Create a name to be part of the resulting package name
            String jarNameForPackage = jarFile.getName().substring( 0, jarFile.getName().lastIndexOf( "." ) );
            jarNameForPackage = jarNameForPackage.replaceAll( "-", "_" );
            jarNameForPackage = jarNameForPackage.replaceAll( "\\.", "_" );
            jarNameForPackage = jarNameForPackage.toLowerCase();

            //Create the rule for this class and add it to the list of rules for this jar
            Rule rule = new Rule();

            String pattern = packageName + ".**";//Example: "org.apache.xerces.dom.**"
            String result = "com.dotcms.repackage." + jarNameForPackage + "." + packageName + ".@1";

            rule.setPattern( pattern );
            rule.setResult( result );

            //Global list of rules to apply
            if ( !rulesToApply.containsKey( pattern + result ) ) {
                rulesToApply.put( pattern + result, rule );
            }

            //Global list of jars to repackage
            if ( !toTransform.contains( jarFile ) ) {
                toTransform.add( jarFile );
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

        //And finally repackage the jars
        generate( getOutputFile(), rulesToApply.values(), toTransform );

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //CHANGE THE OLD REFERENCES IN THE DOTCMS JAR
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++

        //Prepare the jars that we are going to repackage
        Collection<File> files = new ArrayList<File>();
        files.add( dotcmsJar );
        //-------------------------------
        //SOME LOGGING
        logJars( files );

        //And finally repackage the file
        generate( dotcmsJar.getName(), rulesToApply.values(), files );
    }

    /**
     * Repackage a list of given jars applying given rules
     *
     * @param outputFileName the output jar after processing
     * @param rules          rules to apply
     * @param jars           jars to integrate into one
     */
    private void generate ( String outputFileName, Collection<Rule> rules, Collection<File> jars ) {

        //Destiny file
        File outFile = new File( getOutputFolder() + File.separator + outputFileName );
        setDestFile( outFile );

        //Prepare the jars that we are going to repackage
        for ( File jar : jars ) {

            ZipFileSet fileSet = new ZipFileSet();
            fileSet.setSrc( jar );

            //Add it to the fileset
            addZipfileset( fileSet );
        }

        //Prepare the rules for these groups of jar
        for ( Rule rule : rules ) {
            addConfiguredRule( rule );
        }

        //Generate the new jar
        super.execute();
        //Clean everything a e ready to start again
        super.reset();
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

    public void setLibraryPath ( String libraryPath ) {
        this.libraryPath = libraryPath;
    }

    public String getDotcmsJar () {
        return dotcmsJar;
    }

    public void setDotcmsJar ( String dotcmsJar ) {
        this.dotcmsJar = dotcmsJar;
    }

}