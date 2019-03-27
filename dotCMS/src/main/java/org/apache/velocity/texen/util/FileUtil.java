package org.apache.velocity.texen.util;

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

/**
 * A general file utility for use in the context
 *
 * @author <a href="mailto:leon@opticode.co.za">Leon Messerschmidt</a>
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @version $Id: FileUtil.java 463298 2006-10-12 16:10:32Z henning $
 */
public class FileUtil
{
    /**
     * Creates the directory s (and any parent directories needed).
     *
     * @param s path/directory to create.
     * @return report of path/directory creation.
     */
    static public String mkdir (String s)
    {
        try
        {
            if ((new File(s)).mkdirs())
                return "Created dir: "+s;
            else
                return "Failed to create dir or dir already exists: "+s;
        }
        catch (Exception e)
        {
            return e.toString();
        }
    }

    /**
     * A method to get a File object.
     *
     * @param s path to file object to create.
     * @return File created file object.
     */
    public static File file(String s)
    {
        File f = new File(s);
        return f;
    }

    /**
     * A method to get a File object.
     *
     * @param base base path
     * @param s file name
     * @return File created file object.
     */
    public static File file(String base, String s)
    {
        File f = new File(base, s);
        return f;
    }
}
