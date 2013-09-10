package com.dotcms.packager;

import com.tonicsystems.jarjar.JarJarTask;
import com.tonicsystems.jarjar.Rule;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.ZipFileSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Jonathan Gamba
 *         Date: 9/4/13
 */
public class PackagerTask extends JarJarTask {

    private String outputFolder;

    public String getOutputFolder () {
        return outputFolder;
    }

    public void setOutputFolder ( String outputFolder ) {
        this.outputFolder = outputFolder;
    }

    public void execute () throws BuildException {

        //Object with the duplicated classes
        Inspector inspector = InspectorTask.globalInspector;
        //inspector.report( "Test from Packager Task" );

        //Prepare the jars that we are going to repackage
        Collection<File> files = new ArrayList<File>();

        File tikaJar = new File( "/home/jonathan/Projects/dotCMS/repository/git/dotCMS/dotCMS/WEB-INF/lib/tika-app-1.3.jar" );
        files.add( tikaJar );

        //Prepare the rules for these groups of jar
        Collection<Rule> rules = new ArrayList<Rule>();

        Rule rule = new Rule();
        rule.setPattern( "de.l3s.boilerpipe.**" );
        rule.setResult( "org.dotcms.example.@1" );
        rules.add( rule );

        generate( "test1.jar", rules, files );
        generate( "test2.jar", rules, files );
    }

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
        cleanHelper();
    }

}