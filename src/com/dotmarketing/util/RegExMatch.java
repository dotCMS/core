/**
 * 
 */
package com.dotmarketing.util;

import java.util.List;

/**
 * @author Jason Tesser
 * @author Andres Olarte
 *
 */
public class RegExMatch {

	private String match;
	private int begin;
	private int end;
	private List<RegExMatch> groups;
	/**
	 * @return the match
	 */
	public String getMatch() {
		return match;
	}
	/**
	 * @param match the match to set
	 */
	public void setMatch(String match) {
		this.match = match;
	}
	/**
	 * @return the begin
	 */
	public int getBegin() {
		return begin;
	}
	/**
	 * @param begin the begin to set
	 */
	public void setBegin(int begin) {
		this.begin = begin;
	}
	/**
	 * @return the end
	 */
	public int getEnd() {
		return end;
	}
	/**
	 * @param end the end to set
	 */
	public void setEnd(int end) {
		this.end = end;
	}
	/**
	 * @return the groups
	 */
	public List<RegExMatch> getGroups() {
		return groups;
	}
	/**
	 * @param groups the groups to set
	 */
	public void setGroups(List<RegExMatch> groups) {
		this.groups = groups;
	}
	
}
