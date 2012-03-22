/*
 * Copyright 2004,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dotmarketing.scripting.util.php;

import java.util.List;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.program.Function;

/**
 * The serves as the return Object for PHP within dotCMS. 
 * 
 * @author Jason Tesser
 *
 */

public class PHPEvalWrapper {

	private List<Function> functions = null;
	private String out;
	private Value value;
	private QuercusPage page = null;
	
	public PHPEvalWrapper() {
	
	}
	
	public List<Function> getFunctions() {
		return functions;
	}
	public void setFunctions(List<Function> functions) {
		this.functions = functions;
	}
	public String getOut() {
		return out;
	}
	public void setOut(String out) {
		this.out = out;
	}

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
	}
	
	public QuercusPage getPage() {
		return page;
	}

	public void setPage(QuercusPage page) {
		this.page = page;
	}
	
	@Override
	public String toString() {
		String ret = "";
		if(value != null && value.toJavaString().length() > 0){
			ret = value.toJavaString();
		}else if(out != null){
			ret = out;
		}
		return ret;
	}
	
}
