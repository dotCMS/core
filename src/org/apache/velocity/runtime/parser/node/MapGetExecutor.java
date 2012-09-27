package org.apache.velocity.runtime.parser.node;

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

import java.util.Map;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.log.Log;

/**
 * GetExecutor that is smart about Maps. If it detects one, it does not
 * use Reflection but a cast to access the getter. 
 *
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id: MapGetExecutor.java 799457 2009-07-30 22:10:27Z nbubna $
 * @since 1.5
 */
public class MapGetExecutor
        extends AbstractExecutor 
{
    private final String property;

    public MapGetExecutor(final Log log, final Class clazz, final String property)
    {
        this.log = log;
        this.property = property;
        discover(clazz);
    }

    protected void discover (final Class clazz)
    {
        if (property != null && Map.class.isAssignableFrom(clazz))
        {
            try
            {
                setMethod(Map.class.getMethod("get", new Class [] { Object.class }));
            }
            /**
             * pass through application level runtime exceptions
             */
            catch( RuntimeException e )
            {
                throw e;
            }
            catch(Exception e)
            {
                String msg = "Exception while looking for get('" + property + "') method";
                log.error(msg, e);
                throw new VelocityException(msg, e);
            }
        }
    }

    public Object execute(final Object o)
    {
        return ((Map) o).get(property);
    } 
}
