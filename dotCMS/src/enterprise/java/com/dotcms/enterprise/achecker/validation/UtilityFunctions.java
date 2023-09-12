/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.achecker.validation;

import java.util.Arrays;
import java.util.List;

import com.dotcms.enterprise.achecker.validation.FunctionRepository;

public class UtilityFunctions implements FunctionRepository {
	
	public void setGlobalVariable(String name, Object value) {
	}

	public List<String> array1(String a) { return Arrays.asList(a); }

	public List<String> array2(String a, String b) { return Arrays.asList(a, b); }

	public List<String> array3(String a, String b, String c) { return Arrays.asList(a, b, c); }

	public List<String> array4(String a, String b, String c, String d) { return Arrays.asList(a, b, c, d); }

	public List<String> array5(String a, String b, String c, String d, String e) { return Arrays.asList(a, b, c, d, e); }

	public List<String> array6(String a, String b, String c, String d, String e, String f) { return Arrays.asList(a, b, c, d, e, f); }

	public List<String> array7(String a, String b, String c, String d, String e, String f, String g) { return Arrays.asList(a, b, c, d, e, f, g); }

	public List<String> array8(String a, String b, String c, String d, String e, String f, String g, String h) { return Arrays.asList(a, b, c, d, e, f, g, h); }

	public List<String> array9(String a, String b, String c, String d, String e, String f, String g, String h, String i) { return Arrays.asList(a, b, c, d, e, f, g, h, i); }

}
