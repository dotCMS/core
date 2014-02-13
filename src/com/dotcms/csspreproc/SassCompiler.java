package com.dotcms.csspreproc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.jruby.embed.ScriptingContainer;

import com.dotcms.repackage.commons_io_2_0_1.org.apache.commons.io.FileUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.util.FileUtil;

class SassCompiler extends CSSCompiler {

    public SassCompiler(Host host, String uri, boolean live) {
        super(host, uri, live);
    }

    @Override
    public void compile() throws DotSecurityException, DotStateException, DotDataException, IOException {
        final ScriptingContainer jruby=new ScriptingContainer();
        File compileDir=null;
        File config=null;
        File outputDir=null;
        
        try {
            // replace the extension .css with .scss
            String scssUri = inputURI.substring(0, inputURI.lastIndexOf('.')) + ".scss";
            
            // build directories to build scss 
            compileDir = createCompileDir(inputHost, scssUri, inputLive);
            String compileTarget = compileDir.getAbsolutePath() + File.separator + inputHost.getHostname() 
                                   + File.separator + inputURI.substring(0,inputURI.lastIndexOf('/'));
            
            outputDir = new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() 
                    + File.separator + "css_compile_space"
                    + File.separator + UUIDGenerator.generateUuid());
            outputDir.mkdirs();
            
            // write config file
            config=File.createTempFile("compassconf", ".conf");
            PrintWriter configW=new PrintWriter(new FileWriter(config));
            configW.println("sass_dir = \".\"");
            configW.println("css_path = \""+outputDir.getAbsolutePath()+"\"");
            if(inputLive) {
                configW.println("output_style = :compressed ");
            }
            configW.close();
            
            final String compileCode = 
                          "require 'rubygems' \n"+
                          "require 'compass' \n"+
                          "frameworks = Dir.new(Compass::Frameworks::DEFAULT_FRAMEWORKS_PATH).path \n"+
                          "Compass::Frameworks.register_directory(File.join(frameworks, 'compass')) \n"+
                          "Compass::Frameworks.register_directory(File.join(frameworks, 'blueprint')) \n"+
                          "Compass.add_project_configuration '" + config.getAbsolutePath() + "' \n"+
                          "Compass.configure_sass_plugin! \n"+
                          "Dir.chdir('"+ compileTarget +"') do \n"+
                          " Compass.compiler.run \n"+
                          "end \n";
    
            jruby.runScriptlet(compileCode);
                
            File outputFile=new File(outputDir,inputURI.substring(inputURI.lastIndexOf('/')+1));
            if(outputFile.isFile()) {
                output=FileUtils.readFileToByteArray(outputFile);
            }
            else {
                throw new Exception("requested file not found in sass style output directory. sass syntax issue?");
            }
            
        }
        catch(Exception ex) {
            Logger.error(this, "unable to compile sass code "+inputHost.getHostname()+":"+inputURI+" live:"+inputLive,ex);
            throw new RuntimeException(ex);
        }
        finally {
            try {
                jruby.finalize();
            } catch (Throwable e) {
                Logger.warn(this, "error on call to finalize() on jruby scripting container",e);
            }
            
            if(compileDir!=null) FileUtil.deltree(compileDir);
            if(outputDir!=null) FileUtil.deltree(outputDir);
            if(config!=null) config.delete();
            
        }
        
    }

    @Override
    protected String addExtensionIfNeeded(String url) {
        return url.indexOf('.',url.lastIndexOf('/'))!=-1 ? url : 
            url.substring(0, url.lastIndexOf('/'))+ "/_" + url.substring(url.lastIndexOf('/')+1, url.length()) +".scss";
    }

    @Override
    protected String getDefaultExtension() {
        return "scss";
    }

}
