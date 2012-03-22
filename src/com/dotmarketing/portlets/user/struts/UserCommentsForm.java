package com.dotmarketing.portlets.user.struts;

import java.sql.Date;

import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.InodeUtils;

public class UserCommentsForm extends ValidatorForm 
{
	private String inode;
	private String userProxy;
	private Date date;
	private String userComment;
	private String method;
	private String subject;
	private String comment;
    private String commentUserId;
    private String typeComment;

	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getInode() {
		if(InodeUtils.isSet(inode))
			return inode;
		
		return "";
	}
	public void setInode(String inode) {
		this.inode = inode;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getTypeComment() {
		return typeComment;
	}
	public void setTypeComment(String typeComment) {
		this.typeComment = typeComment;
	}
	public String getUserComment() {
		return userComment;
	}
	public void setUserComment(String userComment) {
		this.userComment = userComment;
	}
	public String getUserProxy() {
		return userProxy;
	}
	public void setUserProxy(String userProxy) {
		this.userProxy = userProxy;
	}
	/**
	 * @return Returns the commentUserId.
	 */
	public String getCommentUserId() {
		return commentUserId;
	}
	/**
	 * @param commentUserId The commentUserId to set.
	 */
	public void setCommentUserId(String commentUserId) {
		this.commentUserId = commentUserId;
	}	
}
