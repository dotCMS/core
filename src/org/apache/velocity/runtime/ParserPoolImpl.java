package org.apache.velocity.runtime;

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

import org.apache.velocity.runtime.parser.Parser;
import org.apache.velocity.util.SimplePool;
import org.apache.velocity.runtime.parser.CharStream;

/**
 * This wraps the original parser SimplePool class.  It also handles
 * instantiating ad-hoc parsers if none are available.
 *
 * @author <a href="mailto:sergek@lokitech.com">Serge Knystautas</a>
 * @version $Id: RuntimeInstance.java 384374 2006-03-08 23:19:30Z nbubna $
 * @since 1.5
 */
public class ParserPoolImpl implements ParserPool {

    SimplePool pool = null;
    int max = RuntimeConstants.NUMBER_OF_PARSERS;

    /**
     * Create the underlying "pool".
     * @param rsvc
     */
    public void initialize(RuntimeServices rsvc)
    {
        max = rsvc.getInt(RuntimeConstants.PARSER_POOL_SIZE, RuntimeConstants.NUMBER_OF_PARSERS);
        pool = new SimplePool(max);

        for (int i = 0; i < max; i++)
        {
            pool.put(rsvc.createNewParser());
        }

        if (rsvc.getLog().isDebugEnabled())
        {
            rsvc.getLog().debug("Created '" + max + "' parsers.");
        }
    }

    /**
     * Call the wrapped pool.  If none are available, it will create a new
     * temporary one.
     * @return A parser Object.
     */
    public Parser get()
    {
        return (Parser) pool.get();
    }

    /**
     * Call the wrapped pool.
     * @param parser
     */
    public void put(Parser parser)
    {
        parser.ReInit((CharStream) null);
        pool.put(parser);
    }
}
