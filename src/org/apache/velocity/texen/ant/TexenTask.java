package org.apache.velocity.texen.ant;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.texen.Generator;
import org.apache.velocity.util.StringUtils;

/**
 * An ant task for generating output by using Velocity
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="robertdonkin@mac.com">Robert Burrell Donkin</a>
 * @version $Id: TexenTask.java 463298 2006-10-12 16:10:32Z henning $
 */
public class TexenTask
    extends Task
{
    /**
     * This message fragment (telling users to consult the log or
     * invoke ant with the -debug flag) is appended to rethrown
     * exception messages.
     */
    private final static String ERR_MSG_FRAGMENT =
        ". For more information consult the velocity log, or invoke ant " +
        "with the -debug flag.";

    /**
     * This is the control template that governs the output.
     * It may or may not invoke the services of worker
     * templates.
     */
    protected String controlTemplate;

    /**
     * This is where Velocity will look for templates
     * using the file template loader.
     */
    protected String templatePath;

    /**
     * This is where texen will place all the output
     * that is a product of the generation process.
     */
    protected String outputDirectory;

    /**
     * This is the file where the generated text
     * will be placed.
     */
    protected String outputFile;

    /**
     * This is the encoding for the output file(s).
     */
    protected String outputEncoding;

    /**
     * This is the encoding for the input file(s)
     * (templates).
     */
    protected String inputEncoding;

    /**
     * <p>
     * These are properties that are fed into the
     * initial context from a properties file. This
     * is simply a convenient way to set some values
     * that you wish to make available in the context.
     * </p>
     * <p>
     * These values are not critical, like the template path
     * or output path, but allow a convenient way to
     * set a value that may be specific to a particular
     * generation task.
     * </p>
     * <p>
     * For example, if you are generating scripts to allow
     * user to automatically create a database, then
     * you might want the <code>$databaseName</code>
     * to be placed
     * in the initial context so that it is available
     * in a script that might look something like the
     * following:
     * <code><pre>
     * #!bin/sh
     *
     * echo y | mysqladmin create $databaseName
     * </pre></code>
     * The value of <code>$databaseName</code> isn't critical to
     * output, and you obviously don't want to change
     * the ant task to simply take a database name.
     * So initial context values can be set with
     * properties file.
     */
    protected ExtendedProperties contextProperties;

    /**
     * Property which controls whether the classpath
     * will be used when trying to locate templates.
     */
    protected boolean useClasspath;

    /**
     * The LogFile (incl. path) to log to.
     */
    protected String logFile;

    /**
     *   Property which controls whether the resource
     *   loader will be told to cache.  Default false
     */

    protected String useResourceLoaderCache = "false";
    /**
     *
     */
    protected String resourceLoaderModificationCheckInterval = "2";

    /**
     * [REQUIRED] Set the control template for the
     * generating process.
     * @param controlTemplate
     */
    public void setControlTemplate (String controlTemplate)
    {
        this.controlTemplate = controlTemplate;
    }

    /**
     * Get the control template for the
     * generating process.
     * @return The current control template.
     */
    public String getControlTemplate()
    {
        return controlTemplate;
    }

    /**
     * [REQUIRED] Set the path where Velocity will look
     * for templates using the file template
     * loader.
     * @param templatePath
     * @throws Exception
     */

    public void setTemplatePath(String templatePath) throws Exception
    {
        StringBuffer resolvedPath = new StringBuffer();
        StringTokenizer st = new StringTokenizer(templatePath, ",");
        while ( st.hasMoreTokens() )
        {
            // resolve relative path from basedir and leave
            // absolute path untouched.
            File fullPath = project.resolveFile(st.nextToken());
            resolvedPath.append(fullPath.getCanonicalPath());
            if ( st.hasMoreTokens() )
            {
                resolvedPath.append(",");
            }
        }
        this.templatePath = resolvedPath.toString();

        System.out.println(templatePath);
     }

    /**
     * Get the path where Velocity will look
     * for templates using the file template
     * loader.
     * @return The template path.
     */
    public String getTemplatePath()
    {
        return templatePath;
    }

    /**
     * [REQUIRED] Set the output directory. It will be
     * created if it doesn't exist.
     * @param outputDirectory
     */
    public void setOutputDirectory(File outputDirectory)
    {
        try
        {
            this.outputDirectory = outputDirectory.getCanonicalPath();
        }
        catch (java.io.IOException ioe)
        {
            throw new BuildException(ioe);
        }
    }

    /**
     * Get the output directory.
     * @return The output directory.
     */
    public String getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * [REQUIRED] Set the output file for the
     * generation process.
     * @param outputFile
     */
    public void setOutputFile(String outputFile)
    {
        this.outputFile = outputFile;
    }

    /**
     * Set the output encoding.
     * @param outputEncoding
     */
    public void setOutputEncoding(String outputEncoding)
    {
        this.outputEncoding = outputEncoding;
    }

    /**
     * Set the input (template) encoding.
     * @param inputEncoding
     */
    public void setInputEncoding(String inputEncoding)
    {
        this.inputEncoding = inputEncoding;
    }

    /**
     * Get the output file for the
     * generation process.
     * @return The output file.
     */
    public String getOutputFile()
    {
        return outputFile;
    }

    /**
     * Sets the log file.
     * @param log
     */
    public void setLogFile(String log)
    {
        this.logFile = log;
    }

    /**
     * Gets the log file.
     * @return The log file.
     */
    public String getLogFile()
    {
        return this.logFile;
    }

    /**
     * Set the context properties that will be
     * fed into the initial context be the
     * generating process starts.
     * @param file
     */
    public void setContextProperties( String file )
    {
        String[] sources = StringUtils.split(file,",");
        contextProperties = new ExtendedProperties();

        // Always try to get the context properties resource
        // from a file first. Templates may be taken from a JAR
        // file but the context properties resource may be a
        // resource in the filesystem. If this fails than attempt
        // to get the context properties resource from the
        // classpath.
        for (int i = 0; i < sources.length; i++)
        {
            ExtendedProperties source = new ExtendedProperties();

            try
            {
                // resolve relative path from basedir and leave
                // absolute path untouched.
                File fullPath = project.resolveFile(sources[i]);
                log("Using contextProperties file: " + fullPath);
                source.load(new FileInputStream(fullPath));
            }
            catch (IOException e)
            {
                ClassLoader classLoader = this.getClass().getClassLoader();

                try
                {
                    InputStream inputStream = classLoader.getResourceAsStream(sources[i]);

                    if (inputStream == null)
                    {
                        throw new BuildException("Context properties file " + sources[i] +
                            " could not be found in the file system or on the classpath!");
                    }
                    else
                    {
                        source.load(inputStream);
                    }
                }
                catch (IOException ioe)
                {
                    source = null;
                }
            }

            if (source != null)
            {
                for (Iterator j = source.getKeys(); j.hasNext(); )
                {
                    String name = (String) j.next();
                    String value = StringUtils.nullTrim(source.getString(name));
                    contextProperties.setProperty(name,value);
                }
            }
        }
    }

    /**
     * Get the context properties that will be
     * fed into the initial context be the
     * generating process starts.
     * @return The current context properties.
     */
    public ExtendedProperties getContextProperties()
    {
        return contextProperties;
    }

    /**
     * Set the use of the classpath in locating templates
     *
     * @param useClasspath true means the classpath will be used.
     */
    public void setUseClasspath(boolean useClasspath)
    {
        this.useClasspath = useClasspath;
    }

    /**
     * @param useResourceLoaderCache
     */
    public void setUseResourceLoaderCache(String useResourceLoaderCache)
    {
        this.useResourceLoaderCache = useResourceLoaderCache;
    }

    /**
     * @param resourceLoaderModificationCheckInterval
     */
    public void setResourceLoaderModificationCheckInterval(String resourceLoaderModificationCheckInterval)
    {
        this.resourceLoaderModificationCheckInterval = resourceLoaderModificationCheckInterval;
    }
    /**
     * Creates a VelocityContext.
     *
     * @return new Context
     * @throws Exception the execute method will catch
     *         and rethrow as a <code>BuildException</code>
     */
    public Context initControlContext()
        throws Exception
    {
        return new VelocityContext();
    }

    /**
     * Execute the input script with Velocity
     *
     * @throws BuildException
     * BuildExceptions are thrown when required attributes are missing.
     * Exceptions thrown by Velocity are rethrown as BuildExceptions.
     */
    public void execute ()
        throws BuildException
    {
        // Make sure the template path is set.
        if (templatePath == null && useClasspath == false)
        {
            throw new BuildException(
                "The template path needs to be defined if you are not using " +
                "the classpath for locating templates!");
        }

        // Make sure the control template is set.
        if (controlTemplate == null)
        {
            throw new BuildException("The control template needs to be defined!");
        }

        // Make sure the output directory is set.
        if (outputDirectory == null)
        {
            throw new BuildException("The output directory needs to be defined!");
        }

        // Make sure there is an output file.
        if (outputFile == null)
        {
            throw new BuildException("The output file needs to be defined!");
        }

        VelocityEngine ve = new VelocityEngine();

        try
        {
            // Setup the Velocity Runtime.
            if (templatePath != null)
            {
                log("Using templatePath: " + templatePath, Project.MSG_VERBOSE);
                ve.setProperty(
                    RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templatePath);
            }

            if (useClasspath)
            {
                log("Using classpath");
                ve.addProperty(
                    VelocityEngine.RESOURCE_LOADER, "classpath");

                ve.setProperty(
                    "classpath." + VelocityEngine.RESOURCE_LOADER + ".class",
                        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

                ve.setProperty(
                    "classpath." + VelocityEngine.RESOURCE_LOADER +
                        ".cache", useResourceLoaderCache);

                ve.setProperty(
                    "classpath." + VelocityEngine.RESOURCE_LOADER +
                        ".modificationCheckInterval", resourceLoaderModificationCheckInterval);
            }

            if (this.logFile != null)
            {
                ve.setProperty(RuntimeConstants.RUNTIME_LOG, this.logFile);
            }

            ve.init();

            // Create the text generator.
            Generator generator = Generator.getInstance();
            generator.setVelocityEngine(ve);
            generator.setOutputPath(outputDirectory);
            generator.setInputEncoding(inputEncoding);
            generator.setOutputEncoding(outputEncoding);

            if (templatePath != null)
            {
                generator.setTemplatePath(templatePath);
            }

            // Make sure the output directory exists, if it doesn't
            // then create it.
            File file = new File(outputDirectory);
            if (! file.exists())
            {
                file.mkdirs();
            }

            String path = outputDirectory + File.separator + outputFile;
            log("Generating to file " + path, Project.MSG_INFO);
            Writer writer = generator.getWriter(path, outputEncoding);

            // The generator and the output path should
            // be placed in the init context here and
            // not in the generator class itself.
            Context c = initControlContext();

            // Everything in the generator class should be
            // pulled out and placed in here. What the generator
            // class does can probably be added to the Velocity
            // class and the generator class can probably
            // be removed all together.
            populateInitialContext(c);

            // Feed all the options into the initial
            // control context so they are available
            // in the control/worker templates.
            if (contextProperties != null)
            {
                Iterator i = contextProperties.getKeys();

                while (i.hasNext())
                {
                    String property = (String) i.next();
                    String value = StringUtils.nullTrim(contextProperties.getString(property));

                    // Now lets quickly check to see if what
                    // we have is numeric and try to put it
                    // into the context as an Integer.
                    try
                    {
                        c.put(property, new Integer(value));
                    }
                    catch (NumberFormatException nfe)
                    {
                        // Now we will try to place the value into
                        // the context as a boolean value if it
                        // maps to a valid boolean value.
                        String booleanString =
                            contextProperties.testBoolean(value);

                        if (booleanString != null)
                        {
                            c.put(property, Boolean.valueOf(booleanString));
                        }
                        else
                        {
                            // We are going to do something special
                            // for properties that have a "file.contents"
                            // suffix: for these properties will pull
                            // in the contents of the file and make
                            // them available in the context. So for
                            // a line like the following in a properties file:
                            //
                            // license.file.contents = license.txt
                            //
                            // We will pull in the contents of license.txt
                            // and make it available in the context as
                            // $license. This should make texen a little
                            // more flexible.
                            if (property.endsWith("file.contents"))
                            {
                                // We need to turn the license file from relative to
                                // absolute, and let Ant help :)
                                value = StringUtils.fileContentsToString(
                                    project.resolveFile(value).getCanonicalPath());

                                property = property.substring(
                                    0, property.indexOf("file.contents") - 1);
                            }

                            c.put(property, value);
                        }
                    }
                }
            }

            writer.write(generator.parse(controlTemplate, c));
            writer.flush();
            writer.close();
            generator.shutdown();
            cleanup();
        }
        catch( BuildException e)
        {
            throw e;
        }
        catch( MethodInvocationException e )
        {
            throw new BuildException(
                "Exception thrown by '" + e.getReferenceName() + "." +
                    e.getMethodName() +"'" + ERR_MSG_FRAGMENT,
                        e.getWrappedThrowable());
        }
        catch( ParseErrorException e )
        {
            throw new BuildException("Velocity syntax error" + ERR_MSG_FRAGMENT ,e);
        }
        catch( ResourceNotFoundException e )
        {
            throw new BuildException("Resource not found" + ERR_MSG_FRAGMENT,e);
        }
        catch( Exception e )
        {
            throw new BuildException("Generation failed" + ERR_MSG_FRAGMENT ,e);
        }
    }

    /**
     * <p>Place useful objects into the initial context.</p>
     *
     * <p>TexenTask places <code>Date().toString()</code> into the
     * context as <code>$now</code>.  Subclasses who want to vary the
     * objects in the context should override this method.</p>
     *
     * <p><code>$generator</code> is not put into the context in this
     * method.</p>
     *
     * @param context The context to populate, as retrieved from
     * {@link #initControlContext()}.
     *
     * @throws Exception Error while populating context.  The {@link
     * #execute()} method will catch and rethrow as a
     * <code>BuildException</code>.
     */
    protected void populateInitialContext(Context context)
        throws Exception
    {
        context.put("now", new Date().toString());
    }

    /**
     * A hook method called at the end of {@link #execute()} which can
     * be overridden to perform any necessary cleanup activities (such
     * as the release of database connections, etc.).  By default,
     * does nothing.
     *
     * @exception Exception Problem cleaning up.
     */
    protected void cleanup()
        throws Exception
    {
    }
}
