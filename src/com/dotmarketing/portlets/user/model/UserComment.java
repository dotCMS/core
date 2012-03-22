package com.dotmarketing.portlets.user.model;

import java.io.Serializable;
import java.util.Date;

import com.dotmarketing.beans.Inode;

/** @author Hibernate CodeGenerator */
public class UserComment extends Inode implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public final static String METHOD_EMAIL_CONFIRMATION="email confirmation";
    public final static String METHOD_PHONE="phone";
    public final static  String METHOD_WALKIN="Walk-in";
    public final static  String METHOD_EMAIL="Walk-in";
    public final static String METHOD_REGULAR="Regular";
    public final static  String METHOD_WEB="From Web";
    public final static  String METHOD_MARKETING_LIST="Marketing List";
    public final static String METHOD_OTHER="Other";
    public final static String[] ALL_METHODS={METHOD_PHONE, METHOD_EMAIL, METHOD_REGULAR, METHOD_WEB, METHOD_MARKETING_LIST, METHOD_OTHER};

    public final static String TYPE_INCOMING="incoming";
    public final static  String TYPE_OUTGOING="outgoing";
    public final static  String TYPE_NA="n/a";
    public final static String[] ALL_TYPES={TYPE_INCOMING, TYPE_OUTGOING, TYPE_NA};
        
	/** persistent field */    
    private String userId;
    private String typeComment;
    private String method;
    private String subject;
    private String comment;
    private String commentUserId;
    private Date date;    

    private String communicationId;

    /** default constructor */
    public UserComment() 
    {
    	super.setType("user_comments");
    }

	public String getCommentUserId() {
		return commentUserId;
	}

	public void setCommentUserId(String commentUserId) {
		this.commentUserId = commentUserId;
	}

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

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTypeComment() {
		return typeComment;
	}

	public void setTypeComment(String typeComment) {
		this.typeComment = typeComment;
	}

	/**
	 * @return Returns the communicationId.
	 */
	public String getCommunicationId() {
		return communicationId;
	}

	/**
	 * @param communicationId The communicationId to set.
	 */
	/*public void setCommunicationId(Long communicationId) {
		this.communicationId = communicationId;
	}*/	
	public void setCommunicationId(String communicationId) {
		this.communicationId = communicationId;
	}
}
