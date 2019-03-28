package com.dotmarketing.common.model;


public class  ContentletSearch {
    private String id;
	private String inode;
	private String identifier;
	private String index;
	public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getIndex() {
        return index;
    }
    public void setIndex(String index) {
        this.index = index;
    }
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
    @Override
    public String toString() {
        return "ContentletSearch [id=" + id + ", inode=" + inode + ", identifier=" + identifier + ", index=" + index + ", score=" + score
                + "]";
    }

	
	

}