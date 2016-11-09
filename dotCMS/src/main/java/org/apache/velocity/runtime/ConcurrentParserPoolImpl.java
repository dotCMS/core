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

import org.apache.velocity.runtime.parser.CharStream;
import org.apache.velocity.runtime.parser.Parser;
import org.apache.velocity.util.ConcurrentPool;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class ConcurrentParserPoolImpl implements ParserPool {

	ConcurrentPool<Parser> pool = null;
	int min = Config.getIntProperty("velocity.parser.pool.min", 20);
	int max = Config.getIntProperty("velocity.parser.pool.max", 100);


	/**
	 * Create the underlying "pool".
	 * 
	 * @param rsvc
	 */
	public void initialize(RuntimeServices rsvc) {
		Logger.info(getClass(), "Initializing Velocity Parser Pool - min: " + min + " max:" + max);
		pool = new ConcurrentPool<Parser>(min, max, 30, rsvc);
		if (Logger.isDebugEnabled(this.getClass())) {
			Logger.debug(this, "Created '" + max + "' parsers.");
		}
	}

	/**
	 * Call the wrapped pool. If none are available, it will create a new
	 * temporary one.
	 * 
	 * @return A parser Object.
	 */
	public Parser get() {
		return (Parser) pool.borrowObject();
	}

	/**
	 * Call the wrapped pool.
	 * 
	 * @param parser
	 */
	public void put(Parser parser) {
		parser.ReInit((CharStream) null);
		pool.returnObject(parser);
	}
}
