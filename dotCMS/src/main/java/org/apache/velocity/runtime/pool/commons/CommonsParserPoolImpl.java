package org.apache.velocity.runtime.pool.commons;

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

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.velocity.runtime.ParserPool;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.parser.CharStream;
import org.apache.velocity.runtime.parser.Parser;
import org.apache.velocity.util.ParserFactory;

public class CommonsParserPoolImpl implements ParserPool {

	private static volatile CommonsParserPool pool = null;

	int min = Config.getIntProperty("velocity.parser.pool.min", 20);
	int max = Config.getIntProperty("velocity.parser.pool.max", 100);
	

	/**
	 * Create the underlying "pool".
	 * 
	 * @param rsvc
	 */
	public synchronized void initialize(RuntimeServices rsvc) {


		Logger.info(getClass(), "Initializing Velocity Parser Pool - min: " + min + " max:" + max);

		GenericObjectPoolConfig config = new GenericObjectPoolConfig();

		config.setMinIdle(min);
		config.setMaxIdle(max);
		// configure this?
		config.setMaxTotal(max);
		config.setTimeBetweenEvictionRunsMillis(30000);
		config.setEvictorShutdownTimeoutMillis(5000);
		config.setTestOnBorrow(true);
		config.setTestOnReturn(true);

		pool = new CommonsParserPool(new ParserFactory(rsvc), config);

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
		Parser parser=null;
		try {
			parser=pool.borrowObject();
		//	Logger.getLogger(this.getClass()).info("Parser Pool after borrow="+pool);
		} catch (Exception e) {
			Logger.debug(this, "Error getting parser from pool",e);
		}
		return parser;
	}

	/**
	 * Cleanup and return parser to pool in background
	 * 
	 * @param parser
	 */
	public void put(Parser parser) {
		CompletableFuture.runAsync(() -> {
			parser.ReInit((CharStream) null);
			pool.returnObject(parser);
			//Logger.getLogger(this.getClass()).info("Parser Pool after return="+pool);
		});

	}


	public static synchronized void shutdown() {
		if (pool!=null)
		{
			pool.close();
			pool = null;
		}
	}

}
