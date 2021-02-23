package com.dotmarketing.portlets.user.factories;


import java.util.List;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.user.model.UserComment;
import com.dotmarketing.util.Logger;

/**
 *
 * @author  maria
 */
public class UserCommentsFactory {
	
	private static String COUNT_USER_COMMENTS = "SELECT count(*) as num_rows from user_comments where user_id = ?";

	public static UserComment getComment(String userCommentInode)
	{
		UserComment userComment = (UserComment) InodeFactory.getInode(userCommentInode,UserComment.class);
		return userComment;
	}


	public static List<UserComment> getUserCommentsByProxyInode(String userProxyInode) 
	{
		int limit = 0;
		int offset = 0;
		List<UserComment> userComments = getUserCommentsByProxyInode(userProxyInode,offset,limit);
		return userComments;
    }
	
	public static List<UserComment> getUserCommentsByUserInode(String userInode)
	{
		int offset = 0;
		int limit = 0;
		UserProxy userProxy;
		try {
			userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userInode,APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(UserCommentsFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}	

		return getUserCommentsByProxyInode( userProxy.getInode(),offset,limit);
	}
	
	public static List<UserComment> getUserCommentsByProxyInode(String userProxyInode,int offset,int limit) 
	{
		UserProxy userProxy = (UserProxy) InodeFactory.getInode(userProxyInode,UserProxy.class);
		List<UserComment> userComments = (List<UserComment>) InodeFactory.getChildrenClass(userProxy,UserComment.class,"cdate desc",limit,offset);
		return userComments;
	}

    public static List<UserComment> getLastUserComments(String userProxyInode) 
    {
    	int limit = 5;
    	int offset = 0;
    	List<UserComment> userComments = getUserCommentsByProxyInode(userProxyInode,offset,limit);
		return userComments;        
    }


    public static void deleteUserComment(String userProxyInode,UserComment userComment) throws DotHibernateException 
    {
    	UserProxy userProxy;
		try {
			userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userProxyInode,APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(UserCommentsFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
    	InodeFactory.deleteInode(userComment);
    	userProxy.deleteChild(userComment);
    }
    
    public static void deleteUserComment(String userProxyInode,String userCommentInode) throws DotHibernateException 
    {    	
    	UserComment userComment = UserCommentsFactory.getComment(userCommentInode);
    	deleteUserComment(userProxyInode,userComment);    	    	    	
    }

    public static void saveUserComment(String userProxyInode,UserComment userComment) throws DotHibernateException 
    {
    	UserProxy userProxy;
		try {
			userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userProxyInode,APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(UserCommentsFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}	
    	HibernateUtil.saveOrUpdate(userComment);
    	userProxy.addChild(userComment);
    }
    
    public static void saveUserComment(UserComment userComment) throws DotHibernateException 
    {
    	String userId = userComment.getUserId();    	
    	UserProxy userProxy;
		try {
			userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userId,APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {
			Logger.error(UserCommentsFactory.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
    	HibernateUtil.saveOrUpdate(userComment);
    	userProxy.addChild(userComment);
    }

	public static List<UserComment> getUserCommentsByComm(String userProxyInode, String communicationId) 
	{
		int limit = 0;
		int offset = 0;
		UserProxy userProxy = (UserProxy) InodeFactory.getInode(userProxyInode,UserProxy.class);
		List<UserComment> userComments = (List<UserComment>) InodeFactory.getChildrenClassByCondition(userProxy,UserComment.class,"communication_id = '"+communicationId+"'",limit,offset);
		return userComments;
    }

	public static java.util.List getUserCommentsByUserId(String userId, int offset, int limit) {
        try {
            HibernateUtil dh = new HibernateUtil(UserComment.class);
            dh.setQuery("from user_comments in class com.dotmarketing.portlets.user.model.UserComment where user_id = ? order by cdate desc");
            dh.setParam(userId);
        	dh.setFirstResult(offset);
        	dh.setMaxResults(limit);
            return dh.list();
        } catch (Exception e) {
            Logger.error(UserCommentsFactory.class,"getUserComments failed:" + e);
        }
        return new java.util.ArrayList();
    }

    public static int countUserComments(String UserId){
    	DotConnect db = new DotConnect();
    	db.setSQL(COUNT_USER_COMMENTS);
    	db.addParam(UserId);
    	return db.getInt("num_rows");
	
    }
}
