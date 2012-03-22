package com.dotmarketing.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.oro.text.regex.Perl5Substitution;
import org.apache.oro.text.regex.Util;

import com.dotmarketing.exception.DotRuntimeException;

public class RegEX {

	private ThreadLocal<Perl5Matcher> localP5Matcher = new ThreadLocal<Perl5Matcher>(){
		protected Perl5Matcher initialValue() {
			return new Perl5Matcher();
		}
	};
	
	private ThreadLocal<Perl5Substitution> localP5Sub = new ThreadLocal<Perl5Substitution>(){
		protected Perl5Substitution initialValue() {
			return new Perl5Substitution();
		}
	};
	
	private static RegEX instance;
	private Perl5Compiler compiler;
	
	private Map<String, org.apache.oro.text.regex.Pattern> patterns = new HashMap<String, org.apache.oro.text.regex.Pattern>();
	
	private RegEX() {
		compiler = new Perl5Compiler();
	}
	
	private synchronized static void init(){
		if(instance != null)
			return;
		instance = new RegEX();
	}

	private static RegEX getInstance(){
		if(instance == null){
			init();
		}
		return instance;
	}
	
	private Pattern getPattern(String regEx) throws MalformedPatternException{
		Pattern p = patterns.get(regEx);
		if(!UtilMethods.isSet(p)){
			synchronized (patterns) {
				p = compiler.compile(regEx, Perl5Compiler.READ_ONLY_MASK);
				patterns.put(regEx, p);
			}
		}
		return p;
	}
	
	/**
	 * Will return true/false depending on if the text contains/matches the regEx
	 * @param text
	 * @param regEx Perl5 RegEx
	 * @return
	 * @throws DotRuntimeException
	 */
	public static boolean contains(String text, String regEx)throws DotRuntimeException{
		RegEX i = getInstance();
		Perl5Matcher matcher = i.localP5Matcher.get();
		Pattern pattern;
		try {
			pattern = i.getPattern(regEx);
		} catch (MalformedPatternException e) {
			Logger.error(RegEX.class, "Unable to compile pattern for regex", e);
			throw new DotRuntimeException("Unable to compile pattern for regex",e);
		}
		return matcher.contains(text, pattern);
	}
	
	/**
	 * Will search for the regex pattern within the original string and replace the first occurrence with the substitution
	 * @param original
	 * @param substitution
	 * @param regEx Perl5 RegEx
	 * @return
	 * @throws DotRuntimeException
	 */
	public static String replace(String original,String substitution,String regEx) throws DotRuntimeException{
		String result = original;
		RegEX i = getInstance();
		Perl5Substitution sub = i.localP5Sub.get();
		Perl5Matcher matcher = i.localP5Matcher.get();
		sub.setSubstitution(substitution);
		Pattern pattern;
		try {
			pattern = i.getPattern(regEx);
		} catch (MalformedPatternException e) {
			Logger.error(RegEX.class, "Unable to compile pattern for regex", e);
			throw new DotRuntimeException("Unable to compile pattern for regex",e);
		}
		if(matcher.contains(original, pattern)){
			result = Util.substitute(matcher, pattern, sub, original);
		}
		return result;
	}
	
	/**
	 * Will search for the regex pattern within the original string and replace all the occurrences with the substitution
	 * @param original
	 * @param substitution
	 * @param regEx Perl5 RegEx
	 * @return
	 * @throws DotRuntimeException
	 */
	public static String replaceAll(String original,String substitution,String regEx) throws DotRuntimeException{
		String result = original;
		RegEX i = getInstance();
		Perl5Substitution sub = i.localP5Sub.get();
		Perl5Matcher matcher = i.localP5Matcher.get();
		sub.setSubstitution(substitution);
		Pattern pattern;
		try {
			pattern = i.getPattern(regEx);
		} catch (MalformedPatternException e) {
			Logger.error(RegEX.class, "Unable to compile pattern for regex", e);
			throw new DotRuntimeException("Unable to compile pattern for regex",e);
		}
		if(matcher.contains(original, pattern)){
			result = Util.substitute(matcher, pattern, sub, original, Util.SUBSTITUTE_ALL);
		}
		return result;
	}
	
	public static List<RegExMatch> find(String text, String regEx) throws DotRuntimeException{
		return find(text, regEx, false);
	}
	
	public static List<RegExMatch> findForUrlMap(String text, String regEx) throws DotRuntimeException{
		return find(text, regEx, true);
	}
	
	private static List<RegExMatch> find(String text, String regEx, boolean isUrlMap) throws DotRuntimeException{
		RegEX i = getInstance();
		Perl5Matcher matcher = i.localP5Matcher.get();
		Pattern pattern;
		try {
			pattern = i.getPattern(regEx);
		} catch (MalformedPatternException e) {
			Logger.error(RegEX.class, "Unable to compile pattern for regex", e);
			throw new DotRuntimeException("Unable to compile pattern for regex",e);
		}
		List<RegExMatch> res = new ArrayList<RegExMatch>();
		MatchResult result;
		PatternMatcherInput input = new PatternMatcherInput(text);
		while (matcher.contains(input, pattern)) {
			RegExMatch rm = new RegExMatch();
			result = matcher.getMatch();
			if(!isUrlMap || result.beginOffset(0) == 0){
				rm.setMatch(result.group(0));
				rm.setBegin(result.beginOffset(0));
				rm.setEnd(result.endOffset(0));
				List<RegExMatch> r = new ArrayList<RegExMatch>();
				for(int group = 1; group < result.groups(); group++) {
					RegExMatch rm1 = new RegExMatch();
					rm1.setMatch(result.group(group));
					rm1.setBegin(result.begin(group));
					rm1.setEnd(result.end(group));
					r.add(rm1);
				}
				rm.setGroups(r);
				res.add(rm);
			}

		}
		return res;
	}
}
