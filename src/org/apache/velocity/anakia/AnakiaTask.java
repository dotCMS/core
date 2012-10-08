package org.apache.velocity.anakia;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.util.StringUtils;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.xml.sax.SAXParseException;

/**
 * The purpose of this Ant Task is to allow you to use
 * Velocity as an XML transformation tool like XSLT is.
 * So, instead of using XSLT, you will be able to use this
 * class instead to do your transformations. It works very
 * similar in concept to Ant's &lt;style&gt; task.
 * <p>
 * You can find more documentation about this class on the
 * Velocity
 * <a href="http://velocity.apache.org/engine/devel/docs/anakia.html">Website</a>.
 *
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:szegedia@freemail.hu">Attila Szegedi</a>
 * @version $Id: AnakiaTask.java 501574 2007-01-30 21:32:26Z henning $
 */
public class AnakiaTask extends MatchingTask
{
    /** <code>{@link SAXBuilder}</code> instance to use */
    SAXBuilder builder;

    /** the destination directory */
    private File destDir = null;

    /** the base directory */
    File baseDir = null;

    /** the style= attribute */
    private String style = null;

    /** last modified of the style sheet */
    private long styleSheetLastModified = 0;

    /** the projectFile= attribute */
    private String projectAttribute = null;

    /** the File for the project.xml file */
    private File projectFile = null;

    /** last modified of the project file if it exists */
    private long projectFileLastModified = 0;

    /** check the last modified date on files. defaults to true */
    private boolean lastModifiedCheck = true;

    /** the default output extension is .html */
    private String extension = ".html";

    /** the template path */
    private String templatePath = null;

    /** the file to get the velocity properties file */
    private File velocityPropertiesFile = null;

    /** the VelocityEngine instance to use */
    private VelocityEngine ve = new VelocityEngine();

    /** the Velocity subcontexts */
    private List contexts = new LinkedList();

    /**
     * Constructor creates the SAXBuilder.
     */
    public AnakiaTask()
    {
        builder = new SAXBuilder();
        builder.setFactory(new AnakiaJDOMFactory());
    }

    /**
     * Set the base directory.
     * @param dir
     */
    public void setBasedir(File dir)
    {
        baseDir = dir;
    }

    /**
     * Set the destination directory into which the VSL result
     * files should be copied to
     * @param dir the name of the destination directory
     */
    public void setDestdir(File dir)
    {
        destDir = dir;
    }

    /**
     * Allow people to set the default output file extension
     * @param extension
     */
    public void setExtension(String extension)
    {
        this.extension = extension;
    }

    /**
     * Allow people to set the path to the .vsl file
     * @param style
     */
    public void setStyle(String style)
    {
        this.style = style;
    }

    /**
     * Allow people to set the path to the project.xml file
     * @param projectAttribute
     */
    public void setProjectFile(String projectAttribute)
    {
        this.projectAttribute = projectAttribute;
    }

    /**
     * Set the path to the templates.
     * The way it works is this:
     * If you have a Velocity.properties file defined, this method
     * will <strong>override</strong> whatever is set in the
     * Velocity.properties file. This allows one to not have to define
     * a Velocity.properties file, therefore using Velocity's defaults
     * only.
     * @param templatePath
     */

    public void setTemplatePath(File templatePath)
     {
         try
         {
             this.templatePath = templatePath.getCanonicalPath();
         }
         catch (java.io.IOException ioe)
         {
             throw new BuildException(ioe);
         }
     }

    /**
     * Allow people to set the path to the velocity.properties file
     * This file is found relative to the path where the JVM was run.
     * For example, if build.sh was executed in the ./build directory,
     * then the path would be relative to this directory.
     * This is optional based on the setting of setTemplatePath().
     * @param velocityPropertiesFile
     */
    public void setVelocityPropertiesFile(File velocityPropertiesFile)
    {
        this.velocityPropertiesFile = velocityPropertiesFile;
    }

    /**
     * Turn on/off last modified checking. by default, it is on.
     * @param lastmod
     */
    public void setLastModifiedCheck(String lastmod)
    {
        if (lastmod.equalsIgnoreCase("false") || lastmod.equalsIgnoreCase("no")
                || lastmod.equalsIgnoreCase("off"))
        {
            this.lastModifiedCheck = false;
        }
    }

    /**
     * Main body of the application
     * @throws BuildException
     */
    public void execute () throws BuildException
    {
        DirectoryScanner scanner;
        String[]         list;

        if (baseDir == null)
        {
            baseDir = project.resolveFile(".");
        }
        if (destDir == null )
        {
            String msg = "destdir attribute must be set!";
            throw new BuildException(msg);
        }
        if (style == null)
        {
            throw new BuildException("style attribute must be set!");
        }

        if (velocityPropertiesFile == null)
        {
            velocityPropertiesFile = new File("velocity.properties");
        }

        /*
         * If the props file doesn't exist AND a templatePath hasn't
         * been defined, then throw the exception.
         */
        if ( !velocityPropertiesFile.exists() && templatePath == null )
        {
            throw new BuildException ("No template path and could not " +
                "locate velocity.properties file: " +
                velocityPropertiesFile.getAbsolutePath());
        }

        log("Transforming into: " + destDir.getAbsolutePath(), Project.MSG_INFO);

        // projectFile relative to baseDir
        if (projectAttribute != null && projectAttribute.length() > 0)
        {
            projectFile = new File(baseDir, projectAttribute);
            if (projectFile.exists())
            {
                projectFileLastModified = projectFile.lastModified();
            }
            else
            {
                log ("Project file is defined, but could not be located: " +
                    projectFile.getAbsolutePath(), Project.MSG_INFO );
                projectFile = null;
            }
        }

        Document projectDocument = null;
        try
        {
            if ( velocityPropertiesFile.exists() )
            {
                String file = velocityPropertiesFile.getAbsolutePath();
                ExtendedProperties config = new ExtendedProperties(file);
                ve.setExtendedProperties(config);
            }

            // override the templatePath if it exists
            if (templatePath != null && templatePath.length() > 0)
            {
                ve.setProperty( RuntimeConstants.FILE_RESOURCE_LOADER_PATH,
                    templatePath);
            }

            ve.init();

            // get the last modification of the VSL stylesheet
            styleSheetLastModified = ve.getTemplate( style ).getLastModified();

            // Build the Project file document
            if (projectFile != null)
            {
                projectDocument = builder.build(projectFile);
            }
        }
        catch (Exception e)
        {
            log("Error: " + e.toString(), Project.MSG_INFO);
            throw new BuildException(e);
        }

        // find the files/directories
        scanner = getDirectoryScanner(baseDir);

        // get a list of files to work on
        list = scanner.getIncludedFiles();
        for (int i = 0;i < list.length; ++i)
        {
            process(list[i], projectDocument );
        }

    }

    /**
     * Process an XML file using Velocity
     */
    private void process(String xmlFile, Document projectDocument)
        throws BuildException
    {
        File   outFile=null;
        File   inFile=null;
        Writer writer = null;
        try
        {
            // the current input file relative to the baseDir
            inFile = new File(baseDir,xmlFile);
            // the output file relative to basedir
            outFile = new File(destDir,
                            xmlFile.substring(0,
                            xmlFile.lastIndexOf('.')) + extension);

            // only process files that have changed
            if (lastModifiedCheck == false ||
                    (inFile.lastModified() > outFile.lastModified() ||
                    styleSheetLastModified > outFile.lastModified() ||
                    projectFileLastModified > outFile.lastModified() ||
                    userContextsModifed(outFile.lastModified())))
            {
                ensureDirectoryFor( outFile );

                //-- command line status
                log("Input:  " + xmlFile, Project.MSG_INFO );

                // Build the JDOM Document
                Document root = builder.build(inFile);

                // Shove things into the Context
                VelocityContext context = new VelocityContext();

                /*
                 *  get the property TEMPLATE_ENCODING
                 *  we know it's a string...
                 */
                String encoding = (String) ve.getProperty( RuntimeConstants.OUTPUT_ENCODING );
                if (encoding == null || encoding.length() == 0
                    || encoding.equals("8859-1") || encoding.equals("8859_1"))
                {
                    encoding = "ISO-8859-1";
                }

                Format f = Format.getRawFormat();
                f.setEncoding(encoding);

                OutputWrapper ow = new OutputWrapper(f);

                context.put ("root", root.getRootElement());
                context.put ("xmlout", ow );
                context.put ("relativePath", getRelativePath(xmlFile));
                context.put ("treeWalk", new TreeWalker());
                context.put ("xpath", new XPathTool() );
                context.put ("escape", new Escape() );
                context.put ("date", new java.util.Date() );

                /**
                 * only put this into the context if it exists.
                 */
                if (projectDocument != null)
                {
                    context.put ("project", projectDocument.getRootElement());
                }

                /**
                 *  Add the user subcontexts to the to context
                 */
                for (Iterator iter = contexts.iterator(); iter.hasNext();)
                {
                    Context subContext = (Context) iter.next();
                    if (subContext == null)
                    {
                        throw new BuildException("Found an undefined SubContext!");
                    }

                    if (subContext.getContextDocument() == null)
                    {
                        throw new BuildException("Could not build a subContext for " + subContext.getName());
                    }

                    context.put(subContext.getName(), subContext
                            .getContextDocument().getRootElement());
                }

                /**
                 * Process the VSL template with the context and write out
                 * the result as the outFile.
                 */
                writer = new BufferedWriter(new OutputStreamWriter(
                                            new FileOutputStream(outFile),
                                                encoding));

                /**
                 * get the template to process
                 */
                Template template = ve.getTemplate(style);
                template.merge(context, writer);

                log("Output: " + outFile, Project.MSG_INFO );
            }
        }
        catch (JDOMException e)
        {
            outFile.delete();

            if (e.getCause() != null)
            {
                Throwable rootCause = e.getCause();
                if (rootCause instanceof SAXParseException)
                {
                    System.out.println("");
                    System.out.println("Error: " + rootCause.getMessage());
                    System.out.println(
                        "       Line: " +
                            ((SAXParseException)rootCause).getLineNumber() +
                        " Column: " +
                            ((SAXParseException)rootCause).getColumnNumber());
                    System.out.println("");
                }
                else
                {
                    rootCause.printStackTrace();
                }
            }
            else
            {
                e.printStackTrace();
            }
        }
        catch (Throwable e)
        {
            if (outFile != null)
            {
                outFile.delete();
            }
            e.printStackTrace();
        }
        finally
        {
            if (writer != null)
            {
                try
                {
                    writer.flush();
                }
                catch (IOException e)
                {
                    // Do nothing
                }

                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                    // Do nothing
                }
            }
        }
    }

    /**
     * Hacky method to figure out the relative path
     * that we are currently in. This is good for getting
     * the relative path for images and anchor's.
     */
    private String getRelativePath(String file)
    {
        if (file == null || file.length()==0)
            return "";
        StringTokenizer st = new StringTokenizer(file, "/\\");
        // needs to be -1 cause ST returns 1 even if there are no matches. huh?
        int slashCount = st.countTokens() - 1;
        StringBuffer sb = new StringBuffer();
        for (int i=0;i<slashCount ;i++ )
        {
            sb.append ("../");
        }

        if (sb.toString().length() > 0)
        {
            return StringUtils.chop(sb.toString(), 1);
        }

        return ".";
    }

    /**
     * create directories as needed
     */
    private void ensureDirectoryFor( File targetFile ) throws BuildException
    {
        File directory = new File( targetFile.getParent() );
        if (!directory.exists())
        {
            if (!directory.mkdirs())
            {
                throw new BuildException("Unable to create directory: "
                                         + directory.getAbsolutePath() );
            }
        }
    }


    /**
     * Check to see if user context is modified.
     */
    private boolean userContextsModifed(long lastModified)
    {
        for (Iterator iter = contexts.iterator(); iter.hasNext();)
        {
            AnakiaTask.Context ctx = (AnakiaTask.Context) iter.next();
            if(ctx.getLastModified() > lastModified)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a new context.
     * @return A new context.
     */
    public Context createContext()
    {
        Context context = new Context();
        contexts.add(context);
        return context;
    }


    /**
     * A context implementation that loads all values from an XML file.
     */
    public class Context
    {

        private String name;
        private Document contextDoc = null;
        private String file;

        /**
         * Public constructor.
         */
        public Context()
        {
        }

        /**
         * Get the name of the context.
         * @return The name of the context.
         */
        public String getName()
        {
            return name;
        }

        /**
         * Set the name of the context.
         * @param name
         *
         * @throws IllegalArgumentException if a reserved word is used as a
         * name, specifically any of "relativePath", "treeWalk", "xpath",
         * "escape", "date", or "project"
         */
        public void setName(String name)
        {
            if (name.equals("relativePath") ||
                    name.equals("treeWalk") ||
                    name.equals("xpath") ||
                    name.equals("escape") ||
                    name.equals("date") ||
                    name.equals("project"))
            {

                    throw new IllegalArgumentException("Context name '" + name
                            + "' is reserved by Anakia");
            }

            this.name = name;
        }

        /**
         * Build the context based on a file path.
         * @param file
         */
        public void setFile(String file)
        {
            this.file = file;
        }

        /**
         * Retrieve the time the source file was last modified.
         * @return The time the source file was last modified.
         */
        public long getLastModified()
        {
            return new File(baseDir, file).lastModified();
        }

        /**
         * Retrieve the context document object.
         * @return The context document object.
         */
        public Document getContextDocument()
        {
            if (contextDoc == null)
            {
                File contextFile = new File(baseDir, file);

                try
                {
                    contextDoc = builder.build(contextFile);
                }
                catch (Exception e)
                {
                    throw new BuildException(e);
                }
            }
            return contextDoc;
        }
    }

}
