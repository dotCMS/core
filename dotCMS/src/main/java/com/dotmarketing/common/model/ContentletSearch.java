package com.dotmarketing.common.model;


public class  ContentletSearch {
	private String inode;
	private String identifier;
	float score;
	public float getScore() {
		return score;
	}
	public void setScore(float score) {
		this.score = score;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String getIdentifier() {
		return identifier;
	}
	public void setInode(String inode) {
		this.inode = inode;
	}
	public String getInode() {
		return inode;
	}

}