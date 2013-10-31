package com.tonicsystems.jarjar;

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
    private String jspFolder;
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
        File dotcmsJarFile = new File( this.dotcmsJar );
        log( "dotCMS jar location: " + dotcmsJarFile.getAbsolutePath(), Project.MSG_INFO );

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
        //generate( getOutputFile(), rulesToApply.values(), toTransform, false );

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //CHANGE THE OLD REFERENCES IN THE DOTCMS JAR
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //Repackage the dotcms jar
        //repackageDependent( this.dotcmsJar, rulesToApply );

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //APPLY THE SAME RULES TO GIVEN FILES, XML'S, .PROPERTIES, ETC...
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        if ( filesets != null ) {
            for ( FileSet fileSet : filesets ) {

                if ( fileSet.getDirectoryScanner() != null && fileSet.getDirectoryScanner().getIncludedFiles() != null ) {
                    for ( String file : fileSet.getDirectoryScanner().getIncludedFiles() ) {

                        //File currentFile = new File (file);
                        log( "+++++++++++++++++++++++++++++++++++++++" );
                        log( file );

                        //The file to check and probably to modify
                        String filePath = fileSet.getDirectoryScanner().getBasedir().getPath() + File.separator + file;

                        //Verify if we are going to check for dependencies on another jars or files
                        if ( filePath.endsWith( ".jar" ) ) {

                            //++++++++++++++++++++++++++++++++++++++++++++++++++++++
                            //CHANGE THE OLD REFERENCES IN DEPENDENT JARS
                            //++++++++++++++++++++++++++++++++++++++++++++++++++++++
                            //repackageDependent( filePath, rulesToApply );

                        } else {

                            try {
                                //Reading the file to check
                                FileInputStream inputStream = new FileInputStream( filePath );
                                String fileContent = IOUtils.toString( inputStream );

                                log( "Searching on " + filePath + " for packages strings." );

                                for ( Rule rule : rulesToApply.values() ) {
                                    PackagerWildcard wildcard = new PackagerWildcard( rule );
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

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //APPLY THE SAME RULES TO JSP's UNDER THE DOTCMS FOLDER
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        File jspFolderFile = new File( getJspFolder() );

        for ( Rule rule : rulesToApply.values() ) {

            //Clean-up the pattern
            String pattern = rule.getPattern();
            pattern = pattern.replaceAll( "\\*\\*", "" );

            //Clean up the result
            String result = rule.getResult();
            if ( result.lastIndexOf( "@" ) != -1 ) {
                result = result.substring( 0, result.lastIndexOf( "@" ) );
            }

            String command = "find " + jspFolderFile.getAbsolutePath() + " -name '*.jsp' -exec sed -i 's/" + pattern + "/" + result + "/' {} \\;";

            log( "" );
            log( command );

            try {
                Runtime.getRuntime().exec(
                        new String[]{
                                "sh", "-l", "-c", command
                        }
                );
            } catch ( IOException e ) {
                throw new BuildException( "Error replacing JSP's Strings: " + pattern + ".", e );
            }
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
        String jarLocation = jarFile.getAbsolutePath().substring( 0, jarFile.getAbsolutePath().lastIndexOf( File.separator ) );

        //Prepare the jars that we are going to repackage
        Collection<File> files = new ArrayList<File>();
        files.add( jarFile );
        //-------------------------------
        //SOME LOGGING
        logJars( files );

        //Repackage the jar into a temporal file
        String tempJarName = jarName + "_temp" + ".jar";
        generate( tempJarName, rulesToApply.values(), files, true );

        //Remove the original jar
        jarFile.delete();

        //Rename the just repackaged temporal jar
        File toRename = new File( jarLocation + File.separator + tempJarName );
        File finalJar = new File( jarPath );
        toRename.renameTo( finalJar );
    }

    /**
     * Repackage a list of given jars applying given rules
     *
     * @param outputFileName the output jar after processing
     * @param rules          rules to apply
     * @param jars           jars to integrate into one
     */
    private void generate ( String outputFileName, Collection<Rule> rules, Collection<File> jars, boolean skipManifest ) {

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
        List<PatternElement> patterns = new ArrayList<PatternElement>();
        for ( Rule rule : rules ) {
            addConfiguredRule( rule );
            patterns.add( rule );
        }

        //Generate the new jar
        MainProcessor processor = new MainProcessor( patterns, verbose, skipManifest );
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

    public String getJspFolder () {
        return jspFolder;
    }

    public void setJspFolder ( String jspFolder ) {
        this.jspFolder = jspFolder;
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