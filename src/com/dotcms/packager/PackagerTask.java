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
 *         Date: 9/4/13
 */
public class PackagerTask extends JarJarTask {

    private String libraryPath;
    private String outputFolder;
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

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //PREPARE ALL THE JARS AND RULES TO APPLY
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++

        /*
        What we got here (Something like this):

        1) Duplicated classes
        2) Where is duplicated each class (jars)

        [Plain representation]
            org.bouncycastle.crypto.signers.ISO9796d2PSSSigner
                5204  /home/jonathan/Projects/dotCMS/repository/git/dotCMS/dotCMS/WEB-INF/lib/tika-app-1.3.jar
                5204  /home/jonathan/Projects/dotCMS/repository/git/dotCMS/dotCMS/WEB-INF/lib/bcprov-jdk15-1.45.jar
            com.sun.jna.Function$NativeMappedArray
                993  /home/jonathan/Projects/dotCMS/repository/git/dotCMS/dotCMS/WEB-INF/lib/jna.jar
                1098  /home/jonathan/Projects/dotCMS/repository/git/dotCMS/dotCMS/WEB-INF/lib/jna-3.3.0.jar
            org.dom4j.swing.BranchTreeNode
                2880  /home/jonathan/Projects/dotCMS/repository/git/dotCMS/dotCMS/WEB-INF/lib/tika-app-1.3.jar
                2880  /home/jonathan/Projects/dotCMS/repository/git/dotCMS/dotCMS/WEB-INF/lib/dom4j-1.6.1.jar
            org.dom4j.dom.DOMDocumentType
                4750  /home/jonathan/Projects/dotCMS/repository/git/dotCMS/dotCMS/WEB-INF/lib/tika-app-1.3.jar
                4750  /home/jonathan/Projects/dotCMS/repository/git/dotCMS/dotCMS/WEB-INF/lib/dom4j-1.6.1.jar
            org.apache.wml.WMLWmlElement
                202  /home/jonathan/Projects/dotCMS/repository/git/dotCMS/dotCMS/WEB-INF/lib/tika-app-1.3.jar
                202  /home/jonathan/Projects/dotCMS/repository/git/dotCMS/dotCMS/WEB-INF/lib/xercesImpl.jar
                202  /home/jonathan/Projects/dotCMS/repository/git/dotCMS/dotCMS/WEB-INF/lib/daisydiff.jar

        WHAT WE NEED TO TO:
            A) So witch jar we must modify??, meaning ir we have a class duplicated on 3 or 2 jars we should modify
             just one right?. FOR NOW LEST GRAB THE FIRST ONE..... NOTE: ASK JASON ABOUT THIS

            B) AFTER repackage the chosen jars we must apply the same rules to the dotcms.jar in order to update the dependencies.
         */

        HashMap<File, HashMap<String, Rule>> toTransform = new HashMap<File, HashMap<String, Rule>>();
        HashMap<String, Rule> globalRules = new HashMap<String, Rule>();

        //Get all the duplicated classes
        HashMap<String, List<Inspector.PathInfo>> classes = inspector.getClasses();
        for ( String name : classes.keySet() ) {

            //Collection with the info of where is duplicated
            List<Inspector.PathInfo> details = classes.get( name );
            //if details > 1 means the class is in more than one jar, duplicated!
            if ( details.size() <= 1 ) {
                continue;
            }

            Inspector.PathInfo detail = details.get( 0 );//TODO: Grabbing the first one.....
            File jarFile = detail.base;

            //Handle some strings to use in the rules
            String packageName;
            if ( name.lastIndexOf( "." ) != -1 ) {
                //Get the package of the duplicated class (from my.package.myClassName to my.package)
                packageName = name.substring( 0, name.lastIndexOf( "." ) );
            } else {
                //On the root??, no package??
                continue;
            }

            //Prepare the rules to apply
            HashMap<String, Rule> currentRules;
            if ( toTransform.containsKey( jarFile ) ) {
                currentRules = toTransform.get( jarFile );
            } else {
                currentRules = new HashMap<String, Rule>();
            }

            //Create the rule for this duplicated class and add it to the list of rules for this jar
            Rule rule = new Rule();

            String pattern = packageName + ".**";//Example: "org.apache.xerces.dom.**"
            String result = "com.dotcms." + packageName + ".@1";

            rule.setPattern( pattern );
            rule.setResult( result );
            if ( !currentRules.containsKey( pattern + result ) ) {
                currentRules.put( pattern + result, rule );
            }
            //Add this rule to a global array and have a list of all the applied rules
            if ( !globalRules.containsKey( pattern + result ) ) {
                globalRules.put( pattern + result, rule );
            }

            //Update the map of jars to repackage
            toTransform.put( jarFile, currentRules );
        }

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //NOW REPACKAGE THE JARS
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++

        log( "-----------------------------------------" );
        log( "Jars to repackage: " + toTransform.size() );

        for ( File jarFile : toTransform.keySet() ) {

            //Prepare the jars that we are going to repackage
            Collection<File> files = new ArrayList<File>();
            files.add( jarFile );

            //Prepare the rules for these groups of jar
            Collection<Rule> rules = toTransform.get( jarFile ).values();

            //-------------------------------
            //SOME LOGGING
            logRepackaging( jarFile, rules );

            //And finally repackage the file
            generate( jarFile.getName(), rules, files );
        }

        //++++++++++++++++++++++++++++++++++++++++++++++++++++++
        //CHANGE THE OLD REFERENCES IN THE DOTCMS JAR
        //++++++++++++++++++++++++++++++++++++++++++++++++++++++

        //Prepare the jars that we are going to repackage
        Collection<File> files = new ArrayList<File>();
        files.add( dotcmsJar );

        //Prepare the rules for these groups of jar
        Collection<Rule> rules = globalRules.values();

        //-------------------------------
        //SOME LOGGING
        logRepackaging( dotcmsJar, rules );

        //And finally repackage the file
        generate( dotcmsJar.getName(), rules, files );
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

    private void logRepackaging ( File jar, Collection<Rule> rules ) {

        //-------------------------------
        //SOME LOGGING
        log( "" );
        log( "-----------------------------------------" );
        log( "Repackaging... " + jar.getName() );
        log( "With rules: " );
        for ( Rule rule : rules ) {
            log( rule.getPattern() + " --> " + rule.getResult() );
        }
        //-------------------------------
    }

    public String getOutputFolder () {
        return outputFolder;
    }

    public void setOutputFolder ( String outputFolder ) {
        this.outputFolder = outputFolder;
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