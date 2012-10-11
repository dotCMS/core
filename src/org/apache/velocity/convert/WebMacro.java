package org.apache.velocity.convert;

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
import java.io.FileWriter;
import java.io.IOException;

import org.apache.oro.text.perl.Perl5Util;
import org.apache.velocity.util.StringUtils;
import org.apache.tools.ant.DirectoryScanner;

/**
 * This is deprecated without replacement.
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @version $Id: WebMacro.java 898028 2010-01-11 19:36:31Z nbubna $
 * @deprecated Obsolete and outdated.
 */
public class WebMacro
{
    /**
     *
     */
    protected static final String VM_EXT = ".vm";

    /**
     *
     */
    protected static final String WM_EXT = ".wm";

    /**
     * The regexes to use for line by line substition. The regexes
     * come in pairs. The first is the string to match, the second is
     * the substitution to make.
     */
    protected static String[] perLineREs =
    {
        // Make #if directive match the Velocity directive style.
        "#if\\s*[(]\\s*(.*\\S)\\s*[)]\\s*(#begin|{)[ \\t]?",
        "#if( $1 )",

        // Remove the WM #end #else #begin usage.
        "[ \\t]?(#end|})[ \\t]*\n(\\s*)#else\\s*(#begin|{)[ \\t]?(\\w)",
        "$2#else#**#$4", // avoid touching followup word with embedded comment
        "[ \\t]?(#end|})[ \\t]*\n(\\s*)#else\\s*(#begin|{)[ \\t]?",
        "$2#else",
        "(#end|})(\\s*#else)\\s*(#begin|{)[ \\t]?",
        "$1\n$2",

        // Convert WM style #foreach to Velocity directive style.
        "#foreach\\s+(\\$\\w+)\\s+in\\s+(\\$[^\\s#]+)\\s*(#begin|{)[ \\t]?",
        "#foreach( $1 in $2 )",

        // Convert WM style #set to Velocity directive style.
        "#set\\s+(\\$[^\\s=]+)\\s*=\\s*([\\S \\t]+)",
        "#set( $1 = $2 )",
        "(##[# \\t\\w]*)\\)", // fix comments included at end of line
        ")$1",

        // Convert WM style #parse to Velocity directive style.
        "#parse\\s+([^\\s#]+)[ \\t]?",
        "#parse( $1 )",

        // Convert WM style #include to Velocity directive style.
        "#include\\s+([^\\s#]+)[ \\t]?",
        "#include( $1 )",

        // Convert WM formal reference to VTL syntax.
        "\\$\\(([^\\)]+)\\)",
        "${$1}",
        "\\${([^}\\(]+)\\(([^}]+)}\\)", // fix encapsulated brakets: {(})
        "${$1($2)}",

        // Velocity currently does not permit leading underscore.
        "\\$_",
        "$l_",
        "\\${(_[^}]+)}", // within a formal reference
        "${l$1}",

        // Eat semi-colons in (converted) VTL #set directives.
        "(#set\\s*\\([^;]+);(\\s*\\))",
        "$1$2",

        // Convert explicitly terminated WM statements to VTL syntax.
        "(^|[^\\\\])\\$(\\w[^=\n;'\"]*);",
        "$1${$2}",

        // Change extensions when seen.
        "\\.wm",
        ".vm"
    };

    /**
     * Iterate through the set of find/replace regexes
     * that will convert a given WM template to a VM template
     * @param target
     */
    public void convert(String target)
    {
        File file = new File(target);

        if (!file.exists())
        {
            throw new RuntimeException("The specified template or directory does not exist");
        }

        if (file.isDirectory())
        {
            String basedir = file.getAbsolutePath();
            String newBasedir = basedir + VM_EXT;

            DirectoryScanner ds = new DirectoryScanner();
            ds.setBasedir(basedir);
            ds.addDefaultExcludes();
            ds.scan();
            String[] files = ds.getIncludedFiles();

            for (int i = 0; i < files.length; i++)
            {
                writeTemplate(files[i], basedir, newBasedir);
            }
        }
        else
        {
            writeTemplate(file.getAbsolutePath(), "", "");
        }
    }

    /**
     * Write out the converted template to the given named file
     * and base directory.
     */
    private boolean writeTemplate(String file, String basedir,
                                  String newBasedir)
    {
        if (file.indexOf(WM_EXT) < 0)
        {
            return false;
        }

        System.out.println("Converting " + file + "...");

        String template = file;
        String newTemplate = convertName(file);

        if (basedir.length() > 0)
        {
            String templateDir = newBasedir + extractPath(file);
            File outputDirectory = new File(templateDir);

            template = basedir + File.separator + file;


            if (! outputDirectory.exists())
            {
                outputDirectory.mkdirs();
            }

            newTemplate = newBasedir + File.separator + convertName(file);
        }

        String convertedTemplate = convertTemplate(template);

        FileWriter fw = null;
        try
        {
            fw = new FileWriter(newTemplate);
            fw.write(convertedTemplate);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (fw != null)
            {
                try
                {
                    fw.close();
                }
                catch (IOException io)
                {
                    // Do nothing
                }
            }
        }

        return true;
    }

    /**
     * Gets the path segment of the full path to a file (i.e. one
     * which originally included the file name).
     */
    private final String extractPath(String file)
    {
        int lastSepPos = file.lastIndexOf(File.separator);
        return (lastSepPos == -1 ? "" :
                File.separator + file.substring(0, lastSepPos));
    }

    /**
     * Simple extension conversion of .wm to .vm
     */
    private String convertName(String name)
    {
        return (name.indexOf(WM_EXT) < 0)
                ? name
                : name.substring(0, name.indexOf(WM_EXT)) + VM_EXT;
    }

    /**
     * How to use this little puppy :-)
     */
    private static final void usage()
    {
        System.err.println("Usage: convert-wm <template.wm | directory>");
    }

    /**
     * Apply find/replace regexes to our WM template
     * @param template
     * @return Returns the template with all regexprs applied.
     */
    public String convertTemplate(String template)
    {
        String contents = StringUtils.fileContentsToString(template);

        // Overcome Velocity 0.71 limitation.
        // HELP: Is this still necessary?
        if (!contents.endsWith("\n"))
        {
            contents += "\n";
        }

        // Convert most markup.
        Perl5Util perl = new Perl5Util();
        for (int i = 0; i < perLineREs.length; i += 2)
        {
            contents = perl.substitute(makeSubstRE(i), contents);
        }

        // Convert closing curlies.
        if (perl.match("m/javascript/i", contents))
        {
            // ASSUMPTION: JavaScript is indented, WM is not.
            contents = perl.substitute("s/\n}/\n#end/g", contents);
        }
        else
        {
            contents = perl.substitute("s/(\n\\s*)}/$1#end/g", contents);
            contents = perl.substitute("s/#end\\s*\n\\s*#else/#else/g",
                                       contents);
        }

        return contents;
    }

    /**
     * Makes a Perl 5 regular expression for use by ORO.
     */
    private final String makeSubstRE(int i)
    {
        return ("s/" + perLineREs[i] + '/' + perLineREs[i + 1] + "/g");
    }

    /**
     * Main hook for the conversion process.
     * @param args
     */
    public static void main(String[] args)
    {
        if (args.length > 0)
        {
            for (int x=0; x < args.length; x++)
            {
                WebMacro converter = new WebMacro();
                converter.convert(args[x]);
            }
        }
        else
        {
            usage();
        }
    }
}
