package com.dotcms.packager;

import com.tonicsystems.jarjar.JarJarTask;
import com.tonicsystems.jarjar.Rule;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ZipFileSet;

import java.io.*;
import java.util.*;

/**
 * @author Jonathan Gamba
 *         Date: 10/15/13
 */
public class PackagerTask extends JarJarTask {

    private String libraryPath;
    private String outputFolder;
    private String outputFile;
    private String dotcmsJar;
    private Vector<FileSet> filesets = new Vector<FileSet>();

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

        /*if ( inspector.getDuplicateCount() > 0 ) {
            throw new IllegalStateException( "Found duplicated classes on the specified class path, you need to fix those duplicates " +
                    "in order to continue." );
        }*/

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

        //And finally repackage the dotcms jar
        String dotcmsJarName = dotcmsJar.getName().substring( 0, dotcmsJar.getName().lastIndexOf( "." ) );
        String tempDotcmsJarName = dotcmsJarName + "_temp" + ".jar";
        generate( tempDotcmsJarName, rulesToApply.values(), files );

        //Remove the original dotcms jar
        dotcmsJar.delete();

        //Rename the just repackaged dotcms jar
        File toRename = new File( getOutputFolder() + File.separator + tempDotcmsJarName );
        File finalJar = new File( this.dotcmsJar );
        toRename.renameTo( finalJar );

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //APPLY THE SAME RULES TO GIVEN FILES, XML'S, .PROPERTIES, ETC...
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        if ( filesets != null ) {
            for ( FileSet fileSet : filesets ) {

                if ( fileSet.getDirectoryScanner() != null && fileSet.getDirectoryScanner().getIncludedFiles() != null ) {
                    for ( String file : fileSet.getDirectoryScanner().getIncludedFiles() ) {

                        //The file to check and probably to modify
                        String filePath = fileSet.getDirectoryScanner().getBasedir().getPath() + File.separator + file;
                        try {
                            //Reading the file to check
                            FileInputStream inputStream = new FileInputStream( filePath );
                            String fileContent = IOUtils.toString( inputStream );

                            log( "Searching on " + filePath + " for packages strings." );

                            for ( Rule rule : rulesToApply.values() ) {
                                Wildcard wildcard = new Wildcard( rule );
                                fileContent = wildcard.replace( fileContent );
                            }

                            BufferedWriter writer = new BufferedWriter( new FileWriter( filePath ) );
                            writer.write( fileContent );
                            writer.close();

                        } catch ( FileNotFoundException e ) {
                            throw new BuildException( "File " + filePath + " not found.", e );
                        } catch ( IOException e ) {
                            throw new BuildException( "Error checking and/or modifying " + filePath + ".", e );
                        }
                    }
                }
            }
        }
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
        //Clean everything and get ready to start again
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

    public void addFileset ( FileSet fileset ) {
        filesets.add( fileset );
    }

}