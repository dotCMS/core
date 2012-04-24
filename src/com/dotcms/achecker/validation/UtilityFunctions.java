package com.dotcms.achecker.validation;

import java.util.Arrays;
import java.util.List;

import com.dotcms.achecker.validation.FunctionRepository;

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
